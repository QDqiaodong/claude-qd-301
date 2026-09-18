package com.construction.site.controller;

import com.construction.site.entity.MaterialMovement;
import com.construction.site.service.MaterialMovementService;
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
@RequestMapping("/api/movements")
public class MaterialMovementController {

    private final MaterialMovementService service;

    public MaterialMovementController(MaterialMovementService service) {
        this.service = service;
    }

    @GetMapping
    public List<MaterialMovement> list(@RequestParam(required = false) Long materialId,
                                       @RequestParam(required = false) String direction) {
        return service.query(materialId, direction);
    }

    @PostMapping
    public MaterialMovement create(@RequestBody MaterialMovement form) {
        return service.register(form);
    }

    @PutMapping("/{id}")
    public MaterialMovement update(@PathVariable Long id, @RequestBody MaterialMovement form) {
        return service.modify(id, form);
    }
}
