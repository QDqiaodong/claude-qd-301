package com.construction.site.dto;

import com.construction.site.entity.Material;

/**
 * 材料台账的一行：账面结存和可用余量分成两列摆，不许混。
 * 可用余量 = 账面结存 − 占用中预扣合计，这个数既能再开预扣、也能直接出场。
 */
public class MaterialStockView {

    public Long id;
    public String no;
    public String title;
    public String category;
    public Long yardId;
    /** 账面结存（只由进出场流水累出来） */
    public Integer balance;
    public String state;
    /** 占用中预扣合计（中间量，摆出来方便对账） */
    public long reserved;
    /** 可再预扣、可出场的余量 = balance − reserved */
    public long available;

    public static MaterialStockView of(Material m, long reserved) {
        MaterialStockView v = new MaterialStockView();
        v.id = m.id;
        v.no = m.no;
        v.title = m.title;
        v.category = m.category;
        v.yardId = m.yardId;
        v.balance = m.balance;
        v.state = m.state;
        v.reserved = reserved;
        v.available = (m.balance == null ? 0 : m.balance) - reserved;
        return v;
    }
}
