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
    public List<PourReservation> list(@RequestParam(required = false) Long materialId,
                                      @RequestParam(required = false) Long yardId,
                                      @RequestParam(required = false) String state) {
        return service.query(materialId, yardId, state);
    }

    @PostMapping
    public PourReservation create(@RequestBody PourReservation form) {
        return service.register(form);
    }

    @PutMapping("/{id}")
    public PourReservation update(@PathVariable Long id, @RequestBody PourReservation form) {
        return service.modify(id, form);
    }

    /** 开盘兑现：body 里带出场流水的单号、日期、经办人，数量和材料由预扣单定死 */
    @PostMapping("/{id}/fulfill")
    public PourReservation fulfill(@PathVariable Long id, @RequestBody(required = false) MaterialMovement form) {
        return service.fulfill(id, form);
    }

    /** 作废：body 里可以捎一句原因（泵车故障、计划取消……） */
    @PostMapping("/{id}/void")
    public PourReservation cancel(@PathVariable Long id, @RequestBody(required = false) PourReservation form) {
        return service.cancel(id, form);
    }
}
