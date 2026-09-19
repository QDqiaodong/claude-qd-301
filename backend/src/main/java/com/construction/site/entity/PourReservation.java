package com.construction.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 浇筑配料预扣 —— 浇筑段开盘前，先把钢筋水泥砂石从堆场里占住。
 * 预扣只占「可再预扣余量」，不动账面结存、不写出场流水；
 * 等开盘兑现才补一笔出场流水，取消或泵车故障就作废、把余量还回去。
 * 状态只许单向往前走：占用中 → 已兑现 / 已作废，走一次就锁死。
 */
@Entity
@Table(name = "pour_reservation")
public class PourReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 预扣单号 */
    @Column(nullable = false, length = 32, unique = true)
    public String no;

    /** 预扣哪条材料 */
    @Column(name = "material_id", nullable = false)
    public Long materialId;

    /** 预扣时材料所在的堆场（当时的快照，留着查账） */
    @Column(name = "yard_id", nullable = false)
    public Long yardId;

    /** 预扣数量 */
    @Column(nullable = false)
    public Integer amount;

    /** 计划开盘日 */
    @Column(name = "plan_date")
    public java.time.LocalDate planDate;

    /** 经办人 */
    @Column(nullable = false, length = 32)
    public String handler;

    /** 占用中 / 已兑现 / 已作废 */
    @Column(nullable = false, length = 16)
    public String state;

    /** 兑现时补的那笔出场流水单号（没兑现就是空） */
    @Column(name = "movement_no", length = 32)
    public String movementNo;

    /** 备注：作废原因（泵车故障、计划取消……）或浇筑部位 */
    @Column(length = 128)
    public String note;
}
