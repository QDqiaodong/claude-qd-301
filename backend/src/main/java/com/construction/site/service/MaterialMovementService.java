package com.construction.site.service;

import com.construction.site.dto.BizException;
import com.construction.site.entity.Material;
import com.construction.site.entity.MaterialMovement;
import com.construction.site.repository.MaterialMovementRepository;
import com.construction.site.repository.MaterialRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 进出场流水 —— 这张仓业务的重心。
 * 材料的结存不是手填的，而是进场加、出场减，一笔一笔累出来的。
 */
@Service
public class MaterialMovementService {

    private final MaterialMovementRepository movements;
    private final MaterialRepository materials;

    public MaterialMovementService(MaterialMovementRepository movements, MaterialRepository materials) {
        this.movements = movements;
        this.materials = materials;
    }

    public List<MaterialMovement> query(Long materialId, String direction) {
        if (materialId != null) {
            return movements.findByMaterialIdOrderByIdDesc(materialId);
        }
        if (direction != null && !direction.isBlank()) {
            return movements.findByDirectionOrderByIdDesc(direction);
        }
        return movements.findAllByOrderByIdDesc();
    }

    /** 登记一笔进出场，顺手把材料结存改掉 —— 两件事必须一起成。 */
    @Transactional
    public MaterialMovement register(MaterialMovement form) {
        if (form.no == null || form.no.isBlank()) {
            throw new BizException("流水单号得填");
        }
        if (movements.findByNo(form.no.trim()).isPresent()) {
            throw new BizException("流水单号 " + form.no.trim() + " 已经用过了");
        }
        if (form.amount == null || form.amount <= 0) {
            throw new BizException("进出场数量要大于 0");
        }
        if (!"进场".equals(form.direction) && !"出场".equals(form.direction)) {
            throw new BizException("方向只能是进场或者出场");
        }
        if (form.materialId == null) {
            throw new BizException("这条流水得指明是哪批材料");
        }
        Material material = materials.findById(form.materialId)
                .orElseThrow(() -> new BizException("要记流水的那批材料不存在"));

        int balance = material.balance == null ? 0 : material.balance;
        if ("出场".equals(form.direction)) {
            if (balance < form.amount) {
                throw new BizException("这批材料只剩 " + balance + "，出不了 " + form.amount);
            }
            balance -= form.amount;
        } else {
            balance += form.amount;
        }
        material.balance = balance;
        material.state = balance > 0 ? "在库" : "已清空";
        materials.save(material);

        form.no = form.no.trim();
        form.handler = form.handler == null || form.handler.isBlank() ? "未填" : form.handler;
        return movements.save(form);
    }

    /**
     * 流水一旦记下，方向与数量就锁死了 —— 只能补经办人和日期。
     * 要调整数量，正确做法是补一条反向流水，而不是把这笔改掉。
     */
    @Transactional
    public MaterialMovement modify(Long id, MaterialMovement form) {
        MaterialMovement mv = movements.findById(id).orElseThrow(() -> new BizException("这条流水找不到了"));

        if (form.direction != null && !form.direction.isBlank() && !form.direction.equals(mv.direction)) {
            throw new BizException("流水已经入账，方向不能再改了");
        }
        if (form.amount != null && !form.amount.equals(mv.amount)) {
            throw new BizException("流水已经入账，数量不能再改，要调整请补一笔反向流水");
        }
        if (form.materialId != null && !form.materialId.equals(mv.materialId)) {
            throw new BizException("流水已经入账，不能再换到别的材料上");
        }
        if (form.handler != null && !form.handler.isBlank()) {
            mv.handler = form.handler;
        }
        if (form.moveDate != null) {
            mv.moveDate = form.moveDate;
        }
        return movements.save(mv);
    }
}
