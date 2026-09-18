package com.construction.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 材料进出场流水：进场加结存、出场扣结存，一条一条留痕。 */
@Entity
@Table(name = "material_movement")
public class MaterialMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** 流水单号 */
    @Column(nullable = false, length = 32, unique = true)
    public String no;

    /** 对应哪条材料 */
    @Column(name = "material_id", nullable = false)
    public Long materialId;

    /** 进场 / 出场 */
    @Column(nullable = false, length = 16)
    public String direction;

    /** 数量 */
    @Column(nullable = false)
    public Integer amount;

    /** 进出场日期 */
    @Column(name = "move_date")
    public java.time.LocalDate moveDate;

    /** 现场经办人 */
    @Column(nullable = false, length = 32)
    public String handler;
}
