package com.construction.site.service;

import com.construction.site.dto.BizException;
import com.construction.site.entity.Material;
import com.construction.site.entity.MaterialMovement;
import com.construction.site.entity.PourReservation;
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
 * 预扣成立只占「可再预扣余量」，绝不顺手写出场流水；
 * 开盘兑现才补一笔数量刚好等于预扣的出场流水；
 * 取消或泵车故障就作废，余量原样还回去，不留占着结存又出不了场的死扣。
 */
@Service
public class PourReservationService {

    private final PourReservationRepository reservations;
    private final MaterialRepository materials;
    private final MaterialMovementRepository movements;
    private final YardRepository yards;
    private final SafetyInspectionRepository inspections;

    public PourReservationService(PourReservationRepository reservations,
                                  MaterialRepository materials,
                                  MaterialMovementRepository movements,
                                  YardRepository yards,
                                  SafetyInspectionRepository inspections) {
        this.reservations = reservations;
        this.materials = materials;
        this.movements = movements;
        this.yards = yards;
        this.inspections = inspections;
    }

    public List<PourReservation> query(Long materialId, Long yardId, String state) {
        return reservations.findAllByOrderByIdDesc().stream()
                .filter(r -> materialId == null || materialId.equals(r.materialId))
                .filter(r -> yardId == null || yardId.equals(r.yardId))
                .filter(r -> state == null || state.isBlank() || state.equals(r.state))
                .toList();
    }

    /** 新开预扣：只把余量占住，一笔流水都不写。 */
    @Transactional
    public PourReservation register(PourReservation form) {
        if (form.no == null || form.no.isBlank()) {
            throw new BizException("预扣单号得填");
        }
        if (reservations.findByNo(form.no.trim()).isPresent()) {
            throw new BizException("预扣单号 " + form.no.trim() + " 已经用过了");
        }
        if (form.materialId == null) {
            throw new BizException("预扣得指明是哪批材料");
        }
        Material material = materials.findById(form.materialId)
                .orElseThrow(() -> new BizException("要预扣的那批材料不存在"));
        if (form.yardId == null) {
            throw new BizException("预扣得指明是哪个堆场");
        }
        Yard yard = yards.findById(form.yardId)
                .orElseThrow(() -> new BizException("选的堆场不存在"));
        rejectIfYardBlocked(yard, "新开预扣");
        if (material.yardId == null || !material.yardId.equals(yard.id)) {
            throw new BizException("「" + material.title + "」不在「" + yard.title + "」里，选它实际在的堆场再预扣");
        }
        if (form.amount == null || form.amount <= 0) {
            throw new BizException("预扣数量要大于 0");
        }
        if (form.planDate == null) {
            throw new BizException("计划开盘日得填");
        }

        int available = availableOf(material);
        if (form.amount > available) {
            throw new BizException("这批材料可再预扣的余量只剩 " + available + "，预扣 " + form.amount + " 超了");
        }

        form.no = form.no.trim();
        form.handler = form.handler == null || form.handler.isBlank() ? "未填" : form.handler;
        form.state = "占用中";
        form.movementNo = null;
        return reservations.save(form);
    }

    /** 改预扣：只有占用中的能改，只能动数量、开盘日、经办人和备注，材料不许换。 */
    @Transactional
    public PourReservation modify(Long id, PourReservation form) {
        PourReservation r = mustGet(id);
        if (!"占用中".equals(r.state)) {
            throw new BizException("这条预扣已经" + r.state + "，改不动了");
        }
        if (form.materialId != null && !form.materialId.equals(r.materialId)) {
            throw new BizException("预扣的材料不能换，要换料就先作废这条再新开");
        }
        if (form.amount != null && !form.amount.equals(r.amount)) {
            if (form.amount <= 0) {
                throw new BizException("预扣数量要大于 0");
            }
            if (form.amount > r.amount) {
                // 改大就是多占余量，跟新开一样要看堆场的脸色
                Material material = materials.findById(r.materialId)
                        .orElseThrow(() -> new BizException("预扣挂的那批材料找不到了"));
                rejectIfYardBlocked(currentYard(material, r), "把预扣数量改大");
                // 这条预扣自己的占用先还回来再算，剩下的才是它能涨到的上限
                int room = availableOf(material) + r.amount;
                if (form.amount > room) {
                    throw new BizException("这批材料可再预扣的余量只剩 " + room + "，改不到 " + form.amount);
                }
            }
            r.amount = form.amount;
        }
        if (form.planDate != null) {
            r.planDate = form.planDate;
        }
        if (form.handler != null && !form.handler.isBlank()) {
            r.handler = form.handler;
        }
        if (form.note != null && !form.note.isBlank()) {
            r.note = form.note;
        }
        return reservations.save(r);
    }

    /**
     * 开盘兑现：补一笔出场流水，数量刚好等于预扣数量、材料还是预扣当时那一条。
     * 结存不够、堆场停用、流水单号撞车 —— 任何一种情况都整笔停在占用中，
     * 不会一半出了账、一半还挂着预扣。
     */
    @Transactional
    public PourReservation fulfill(Long id, MaterialMovement form) {
        PourReservation r = mustGet(id);
        if ("已作废".equals(r.state)) {
            throw new BizException("这条预扣已经作废了，不能再拿去兑现");
        }
        if ("已兑现".equals(r.state)) {
            throw new BizException("这条预扣已经兑现过了（流水 " + r.movementNo + "），不能重复兑现");
        }

        Material material = materials.findById(r.materialId)
                .orElseThrow(() -> new BizException("预扣挂的那批材料找不到了"));
        Yard yard = currentYard(material, r);
        if ("停用".equals(yard.state)) {
            throw new BizException("堆场「" + yard.title + "」已经停用，兑现不了，预扣继续占用中");
        }
        int balance = material.balance == null ? 0 : material.balance;
        if (balance < r.amount) {
            throw new BizException("结存只剩 " + balance + "，不够兑现 " + r.amount + "，预扣继续占用中");
        }
        if (form == null || form.no == null || form.no.isBlank()) {
            throw new BizException("兑现得给那笔出场流水一个单号");
        }
        String movementNo = form.no.trim();
        if (movements.findByNo(movementNo).isPresent()) {
            throw new BizException("流水单号 " + movementNo + " 撞车了，预扣还在占用中，换个单号再兑现");
        }

        // 流水的数量、方向、材料全由预扣单定死，外面传不进来
        MaterialMovement mv = new MaterialMovement();
        mv.no = movementNo;
        mv.materialId = r.materialId;
        mv.direction = "出场";
        mv.amount = r.amount;
        mv.moveDate = form.moveDate != null ? form.moveDate : java.time.LocalDate.now();
        mv.handler = form.handler == null || form.handler.isBlank() ? r.handler : form.handler;
        movements.save(mv);

        material.balance = balance - r.amount;
        material.state = material.balance > 0 ? "在库" : "已清空";
        materials.save(material);

        r.state = "已兑现";
        r.movementNo = mv.no;
        return reservations.save(r);
    }

    /** 作废：泵车坏了、下雨了、计划取消 —— 余量当场还回去，不留死扣。 */
    @Transactional
    public PourReservation cancel(Long id, PourReservation form) {
        PourReservation r = mustGet(id);
        if ("已兑现".equals(r.state)) {
            throw new BizException("这条预扣已经兑现出场了，不能作废回头重新占余量");
        }
        if ("已作废".equals(r.state)) {
            throw new BizException("这条预扣已经作废过了");
        }
        r.state = "已作废";
        if (form != null && form.note != null && !form.note.isBlank()) {
            r.note = form.note.trim();
        }
        return reservations.save(r);
    }

    private PourReservation mustGet(Long id) {
        return reservations.findById(id).orElseThrow(() -> new BizException("这条预扣找不到了"));
    }

    /** 可再预扣、也可出场的余量 = 账面结存 − 占用中的预扣合计 */
    private int availableOf(Material material) {
        int balance = material.balance == null ? 0 : material.balance;
        Long occupied = reservations.sumOccupiedByMaterialId(material.id);
        return balance - (occupied == null ? 0 : occupied.intValue());
    }

    /** 材料现在待在哪个堆场，就按哪个堆场的规矩来；材料没挂堆场时退回预扣当时的快照 */
    private Yard currentYard(Material material, PourReservation r) {
        Long yardId = material.yardId != null ? material.yardId : r.yardId;
        return yards.findById(yardId).orElseThrow(() -> new BizException("材料所在的堆场找不到了"));
    }

    /** 堆场停用、或者还挂着待整改的不合格巡检 —— 这两种情况下不许做这个动作 */
    private void rejectIfYardBlocked(Yard yard, String action) {
        if ("停用".equals(yard.state)) {
            throw new BizException("堆场「" + yard.title + "」停用了，不能" + action);
        }
        if (inspections.existsByYardIdAndVerdictAndState(yard.id, "不合格", "待整改")) {
            throw new BizException("堆场「" + yard.title + "」还挂着待整改的不合格巡检，不能" + action);
        }
    }
}
