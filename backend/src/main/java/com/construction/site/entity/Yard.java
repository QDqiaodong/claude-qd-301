package com.construction.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 材料堆场 —— 工地上按区域划出来的堆料场地。
 * 一个堆场停用之前，里面堆着的材料必须先清空。
 */
@Entity
@Table(name = "yard")
public class Yard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 堆场编号（Y-01 这种），全工地唯一 */
    @Column(nullable = false, length = 32, unique = true)
    public String no;

    /** 堆场名称 */
    @Column(nullable = false, length = 64)
    public String title;

    /** 占地面积，单位平方米 */
    @Column(name = "area_size")
    public Integer areaSize;

    /** 最多能堆多少件材料 */
    @Column(name = "max_load")
    public Integer maxLoad;

    /** 可用 / 停用 */
    @Column(nullable = false, length = 16)
    public String state;
}
