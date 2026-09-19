package com.construction.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 浇筑配料预扣 —— 浇筑段开盘前，先把钢筋水泥砂石从堆场里占住。
 * 预扣只压「可用余量」，不动账面结存、也不提前写出场流水：
 * 开盘兑现时才补一笔等量出场流水；泵车坏了、下雨了就把预扣作废，余量原样还回。
 * 一条预扣从「占用中」走到「已兑现」或「已作废」只能走一次，回不了头。
 */
@Entity
@Table(name = "pour_reservation")
public class PourReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 预扣单号（PR-01 这种），全工地唯一 */
    @Column(nullable = false, length = 32, unique = true)
    public String no;

    /** 从哪个堆场占料 */
    @Column(name = "yard_id", nullable = false)
    public Long yardId;

    /** 占哪批材料 */
    @Column(name = "material_id", nullable = false)
    public Long materialId;

    /** 预扣数量 */
    @Column(nullable = false)
    public Integer amount;

    /** 计划开盘日 */
    @Column(name = "plan_date")
    public LocalDate planDate;

    /** 占用中 / 已兑现 / 已作废 */
    @Column(nullable = false, length = 16)
    public String state;

    /** 兑现时补的那笔出场流水单号（占用中、已作废时为空） */
    @Column(name = "movement_no", length = 32)
    public String movementNo;
}
