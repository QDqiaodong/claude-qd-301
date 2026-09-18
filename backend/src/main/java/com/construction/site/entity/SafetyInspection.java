package com.construction.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 安全巡检记录：安全员到堆场检查，打分判定，不合格的要整改到闭环。 */
@Entity
@Table(name = "safety_inspection")
public class SafetyInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 巡检单号 */
    @Column(nullable = false, length = 32, unique = true)
    public String no;

    /** 巡的是哪个堆场 */
    @Column(name = "yard_id", nullable = false)
    public Long yardId;

    /** 巡检日期 */
    @Column(name = "inspect_date")
    public java.time.LocalDate inspectDate;

    /** 巡检人 */
    @Column(nullable = false, length = 32)
    public String inspector;

    /** 得分 0~100 */
    @Column(nullable = false)
    public Integer score;

    /** 合格 / 不合格 */
    @Column(nullable = false, length = 16)
    public String verdict;

    /** 待整改 / 已闭环 */
    @Column(nullable = false, length = 16)
    public String state;
}
