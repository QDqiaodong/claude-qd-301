package com.construction.site.service;

import com.construction.site.dto.BizException;
import com.construction.site.dto.MaterialStockView;
import com.construction.site.entity.Material;
import com.construction.site.entity.Yard;
import com.construction.site.repository.MaterialRepository;
import com.construction.site.repository.PourReservationRepository;
import com.construction.site.repository.YardRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialRepository materials;
    private final YardRepository yards;
    private final PourReservationRepository reservations;

    public MaterialService(MaterialRepository materials, YardRepository yards,
                           PourReservationRepository reservations) {
        this.materials = materials;
        this.yards = yards;
        this.reservations = reservations;
    }

    /**
     * 台账每一行都带三个数：账面结存、占用中预扣、可用余量（能再预扣也能出场）。
     * 账面结存和可用余量是两列，不许混着看。
     */
    public List<MaterialStockView> query(Long yardId, String state, String keyword) {
        Map<Long, Long> occupied = reservations.sumOccupiedGroupByMaterial().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
        return materials.findAllByOrderByIdAsc().stream()
                .filter(m -> yardId == null || yardId.equals(m.yardId))
                .filter(m -> state == null || state.isBlank() || state.equals(m.state))
                .filter(m -> keyword == null || keyword.isBlank()
                        || m.no.contains(keyword.trim()) || m.title.contains(keyword.trim()))
                .map(m -> MaterialStockView.of(m, occupied.getOrDefault(m.id, 0L)))
                .toList();
    }

    /**
     * 新增材料。结存一律从 0 起 —— 想让它有货就去登记一条进场流水，
     * 不允许在这里直接塞一个初始结存，否则账实就对不上了。
     */
    @Transactional
    public Material register(Material form) {
        if (form.no == null || form.no.isBlank()) {
            throw new BizException("材料编号得填");
        }
        if (form.title == null || form.title.isBlank()) {
            throw new BizException("材料名称得填");
        }
        if (materials.findByNo(form.no.trim()).isPresent()) {
            throw new BizException("编号 " + form.no.trim() + " 已经用过了");
        }
        form.yardId = resolveYard(form.yardId);
        form.no = form.no.trim();
        form.balance = 0;
        form.state = "已清空";
        return materials.save(form);
    }

    @Transactional
    public Material modify(Long id, Material form) {
        Material m = materials.findById(id).orElseThrow(() -> new BizException("这条材料找不到了"));

        if (form.yardId != null && !form.yardId.equals(m.yardId)) {
            m.yardId = resolveYard(form.yardId);
        }
        if (form.title != null && !form.title.isBlank()) {
            m.title = form.title;
        }
        if (form.category != null && !form.category.isBlank()) {
            m.category = form.category;
        }
        // balance 不在这里改：它只跟着进出场流水走
        if (form.state != null && !form.state.isBlank()) {
            if ("已清空".equals(form.state) && m.balance != null && m.balance > 0) {
                throw new BizException("结存还有 " + m.balance + "，不能标成已清空");
            }
            m.state = form.state;
        }
        return materials.save(m);
    }

    /** 材料只能挂到「可用」的堆场里。 */
    private Long resolveYard(Long yardId) {
        if (yardId == null) {
            return null;
        }
        Yard yard = yards.findById(yardId).orElseThrow(() -> new BizException("要挂的堆场不存在"));
        if ("停用".equals(yard.state)) {
            throw new BizException("堆场「" + yard.title + "」停用了，材料不能往里放");
        }
        return yard.id;
    }
}
