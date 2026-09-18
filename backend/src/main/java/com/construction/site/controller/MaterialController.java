package com.construction.site.controller;

import com.construction.site.entity.Material;
import com.construction.site.service.MaterialService;
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
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @GetMapping
    public List<Material> list(@RequestParam(required = false) Long yardId,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String keyword) {
        return service.query(yardId, state, keyword);
    }

    @PostMapping
    public Material create(@RequestBody Material form) {
        return service.register(form);
    }

    @PutMapping("/{id}")
    public Material update(@PathVariable Long id, @RequestBody Material form) {
        return service.modify(id, form);
    }
}
