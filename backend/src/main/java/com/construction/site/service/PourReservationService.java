package com.construction.site.service;

import com.construction.site.dto.BizException;
import com.construction.site.entity.Material;
import com.construction.site.entity.MaterialMovement;
import com.construction.site.entity.PourReservation;
import com.construction.site.entity.SafetyInspection;
import com.construction.site.entity.Yard;
import com.construction.site.repository.MaterialMovementRepository;
import com.construction.site.repository.MaterialRepository;
import com.construction.site.repository.PourReservationRepository;
import com.construction.site.repository.SafetyInspectionRepository;
import com.construction.site.repository.YardRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 浇筑配料预扣 —— 按施工员那套来：
 * 预扣成立只占「可用余量」，不写出场流水；开盘兑现时才补一笔等量出场流水；
 * 取消、泵车故障、下雨就把预扣作废，余量当场还回 —— 不留占着结存又出不了场的死扣。
 *
 * 状态机：占用中 → 已兑现 / 已作废，只走一次。
 * 作废过的不能再兑现，兑现过的不能再作废回头重新占余量。
 */
@Service
public class PourReservationService {

    private final PourReservationRepository reservations;
    private final MaterialRepository materials;
    private final MaterialMovementRepository movements;
    private final YardRepository yards;
    private final SafetyInspectionRepository inspections;
    private final MaterialMovementService movementService;

    public PourReservationService(PourReservationRepository reservations,
                                  MaterialRepository materials,
                                  MaterialMovementRepository movements,
                                  YardRepository yards,
                                  SafetyInspectionRepository inspections,
                                  MaterialMovementService movementService) {
        this.reservations = reservations;
        this.materials = materials;
        this.movements = movements;
        this.yards = yards;
        this.inspections = inspections;
        this.movementService = movementService;
    }

    public List<PourReservation> query(Long yardId, Long materialId, String state) {
        return reservations.findAllByOrderByIdDesc().stream()
                .filter(r -> yardId == null || yardId.equals(r.yardId))
                .filter(r -> materialId == null || materialId.equals(r.materialId))
                .filter(r -> state == null || state.isBlank() || state.equals(r.state))
                .toList();
    }

    /** 这批材料被「占用中」的预扣一共压着多少 —— 台账余量、出场拦截都用这个口径。 */
    public int occupiedOf(Long materialId) {
        return reservations.findByMaterialIdAndState(materialId, "占用中").stream()
                .mapToInt(r -> r.amount == null ? 0 : r.amount)
                .sum();
    }

    /**
     * 新开预扣：只占余量，不写流水。
     * 堆场停用、或挂着待整改的不合格巡检，直接拒绝；数量超了当场报还剩多少。
     */
    @Transactional
    public PourReservation reserve(PourReservation form) {
        if (form.no == null || form.no.isBlank()) {
            throw new BizException("预扣单号得填");
        }
        if (reservations.findByNo(form.no.trim()).isPresent()) {
            throw new BizException("预扣单号 " + form.no.trim() + " 已经用过了");
        }
        if (form.amount == null || form.amount <= 0) {
            throw new BizException("预扣数量要大于 0");
        }
        if (form.planDate == null) {
            throw new BizException("计划开盘日得填");
        }
        if (form.yardId == null) {
            throw new BizException("预扣得写明从哪个堆场占料");
        }
        if (form.materialId == null) {
            throw new BizException("预扣得写明占哪批材料");
        }

        Yard yard = yards.findById(form.yardId).orElseThrow(() -> new BizException("要占料的堆场不存在"));
        if ("停用".equals(yard.state)) {
            throw new BizException("堆场「" + yard.title + "」停用了，不能新开预扣");
        }
        requireNoPendingRectification(yard, "新开预扣");

        Material material = materials.findById(form.materialId)
                .orElseThrow(() -> new BizException("要占的那批材料不存在"));
        if (!yard.id.equals(material.yardId)) {
            throw new BizException("「" + material.title + "」不在" + yard.title + "，去它所在的堆场占");
        }

        int balance = balanceOf(material);
        int occupied = occupiedOf(material.id);
        int available = balance - occupied;
        if (form.amount > available) {
            throw new BizException("可再预扣的余量只剩 " + available
                    + "（账面结存 " + balance + "，占用中预扣 " + occupied + "），扣不了 " + form.amount);
        }

        form.id = null;
        form.no = form.no.trim();
        form.yardId = yard.id;
        form.materialId = material.id;
        form.state = "占用中";
        form.movementNo = null;
        return reservations.save(form);
    }

    /**
     * 改预扣：只有占用中的能改，只能动数量和计划开盘日，材料与堆场换不了。
     * 数量改大要重新过一遍堆场门禁和余量校验；改小随时行，余量当场还回。
     */
    @Transactional
    public PourReservation modify(Long id, PourReservation form) {
        PourReservation r = reservations.findById(id).orElseThrow(() -> new BizException("这条预扣找不到了"));
        if (!"占用中".equals(r.state)) {
            throw new BizException("只有「占用中」的预扣能改，这条已经是「" + r.state + "」了");
        }
        if (form.materialId != null && !form.materialId.equals(r.materialId)) {
            throw new BizException("预扣占的是哪批材料不能换，要换先作废再重新预扣");
        }
        if (form.yardId != null && !form.yardId.equals(r.yardId)) {
            throw new BizException("从哪个堆场占料不能换，要换先作废再重新预扣");
        }

        if (form.amount != null && !form.amount.equals(r.amount)) {
            if (form.amount <= 0) {
                throw new BizException("预扣数量要大于 0");
            }
            if (form.amount > r.amount) {
                // 改大 = 多占余量，门禁和新开时一个标准
                Yard yard = yards.findById(r.yardId).orElseThrow(() -> new BizException("预扣对应的堆场找不到了"));
                if ("停用".equals(yard.state)) {
                    throw new BizException("堆场「" + yard.title + "」停用了，预扣数量不能改大");
                }
                requireNoPendingRectification(yard, "把预扣数量改大");
            }
            Material material = materials.findById(r.materialId)
                    .orElseThrow(() -> new BizException("预扣对应的那批材料找不到了"));
            int others = occupiedOf(material.id) - r.amount;
            int max = balanceOf(material) - others;
            if (form.amount > max) {
                throw new BizException("这条预扣最多能占到 " + max
                        + "（账面结存 " + balanceOf(material) + "，别的预扣占着 " + others + "），改不到 " + form.amount);
            }
            r.amount = form.amount;
        }
        if (form.planDate != null) {
            r.planDate = form.planDate;
        }
        return reservations.save(r);
    }

    /**
     * 开盘兑现：把占用转成一笔出场流水 —— 数量刚好等于预扣数量、方向只能是出场、
     * 材料还是预扣当时那一条。结存不够、堆场已停用、流水单号撞车，任何一个出了，
     * 整笔回滚，预扣停在占用中，不会一半出了账一半还挂着。
     */
    @Transactional
    public PourReservation fulfill(Long id, MaterialMovement form) {
        PourReservation r = reservations.findById(id).orElseThrow(() -> new BizException("这条预扣找不到了"));
        if ("已作废".equals(r.state)) {
            throw new BizException("作废过的预扣不能再拿去兑现");
        }
        if ("已兑现".equals(r.state)) {
            throw new BizException("这条预扣已经兑现过了，不能重复兑现");
        }

        Material material = materials.findById(r.materialId)
                .orElseThrow(() -> new BizException("预扣对应的那批材料找不到了"));
        Yard yard = yards.findById(r.yardId).orElseThrow(() -> new BizException("预扣对应的堆场找不到了"));
        if ("停用".equals(yard.state)) {
            throw new BizException("堆场「" + yard.title + "」已经停用，兑现先停住，预扣还挂在占用中");
        }

        int balance = balanceOf(material);
        int others = occupiedOf(material.id) - r.amount;
        if (balance - others < r.amount) {
            throw new BizException("账面结存 " + balance + " 扣掉其它占用后不够兑现 " + r.amount
                    + "，兑现没成，预扣还挂在占用中");
        }

        if (form.no == null || form.no.isBlank()) {
            throw new BizException("兑现要补一笔出场流水，流水单号得填");
        }
        String movementNo = form.no.trim();
        if (movements.findByNo(movementNo).isPresent()) {
            throw new BizException("流水单号 " + movementNo + " 已经用过了，兑现没成，预扣还挂在占用中");
        }

        // 先落状态再补流水，同一个事务里：流水那边一旦出问题，两边一起回滚
        r.state = "已兑现";
        r.movementNo = movementNo;
        reservations.save(r);

        MaterialMovement mv = new MaterialMovement();
        mv.no = movementNo;
        mv.materialId = r.materialId;
        mv.direction = "出场";
        mv.amount = r.amount;
        mv.moveDate = form.moveDate;
        mv.handler = form.handler;
        movementService.register(mv);
        return r;
    }

    /** 作废：开盘取消、泵车坏了、下雨了 —— 占着的余量当场还回，不留死扣。 */
    @Transactional
    public PourReservation cancel(Long id) {
        PourReservation r = reservations.findById(id).orElseThrow(() -> new BizException("这条预扣找不到了"));
        if ("已兑现".equals(r.state)) {
            throw new BizException("这条预扣已经兑现出场了，不能作废回头；要冲正就去补一笔进场流水");
        }
        if ("已作废".equals(r.state)) {
            throw new BizException("这条预扣已经作废过了");
        }
        r.state = "已作废";
        return reservations.save(r);
    }

    /** 堆场挂着「待整改」的不合格巡检时，不许新开预扣，也不许把数量改大。 */
    private void requireNoPendingRectification(Yard yard, String action) {
        List<SafetyInspection> pending = inspections.findByYardIdOrderByIdDesc(yard.id).stream()
                .filter(i -> "不合格".equals(i.verdict) && "待整改".equals(i.state))
                .toList();
        if (!pending.isEmpty()) {
            throw new BizException("堆场「" + yard.title + "」还挂着待整改的不合格巡检（"
                    + pending.get(0).no + "），整改闭环前不能" + action);
        }
    }

    private int balanceOf(Material material) {
        return material.balance == null ? 0 : material.balance;
    }
}
