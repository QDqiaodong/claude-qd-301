package com.construction.site.controller;

import com.construction.site.entity.MaterialMovement;
import com.construction.site.entity.PourReservation;
import com.construction.site.service.PourReservationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class PourReservationController {

    private final PourReservationService service;

    public PourReservationController(PourReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<PourReservation> list(@RequestParam(required = false) Long yardId,
                                      @RequestParam(required = false) Long materialId,
                                      @RequestParam(required = false) String state) {
        return service.query(yardId, materialId, state);
    }

    /** 新开预扣：只占余量，不写出场流水 */
    @PostMapping
    public PourReservation create(@RequestBody PourReservation form) {
        return service.reserve(form);
    }

    /** 改预扣数量 / 计划开盘日（只有占用中的能改） */
    @PutMapping("/{id}")
    public PourReservation update(@PathVariable Long id, @RequestBody PourReservation form) {
        return service.modify(id, form);
    }

    /** 开盘兑现：补一笔等量出场流水，整笔要么全成要么留在占用中 */
    @PostMapping("/{id}/fulfill")
    public PourReservation fulfill(@PathVariable Long id, @RequestBody MaterialMovement form) {
        return service.fulfill(id, form);
    }

    /** 作废：开盘取消 / 泵车故障 / 下雨，把占着的余量还回去 */
    @PostMapping("/{id}/void")
    public PourReservation voidIt(@PathVariable Long id) {
        return service.cancel(id);
    }
}
