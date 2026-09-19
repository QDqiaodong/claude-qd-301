package com.construction.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 材料 —— 堆在某个堆场里的钢筋、水泥、砂石。
 * 注意 balance（结存）不是随便改的，它由「进出场流水」累计出来。
 */
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 材料编号 */
    @Column(nullable = false, length = 32, unique = true)
    public String no;

    /** 材料名称 */
    @Column(nullable = false, length = 64)
    public String title;

    /** 类别：钢筋 / 水泥 / 砂石 / 模板 */
    @Column(nullable = false, length = 16)
    public String category;

    /** 堆在哪个堆场 */
    @Column(name = "yard_id")
    public Long yardId;

    /** 当前结存（进场累加、出场扣减，不能手改） */
    @Column(nullable = false)
    public Integer balance;

    /** 在库 / 已清空 */
    @Column(nullable = false, length = 16)
    public String state;

    /** 占用中的浇筑预扣合计（台账展示用，不落库，由预扣单现算） */
    @Transient
    public Integer occupied;

    /**
     * 可用余量 = 账面结存 − 占用中预扣（台账展示用，不落库）。
     * 还能再预扣、还能出场的都是这个数；和账面结存是两列，别混。
     */
    @Transient
    public Integer available;
}
