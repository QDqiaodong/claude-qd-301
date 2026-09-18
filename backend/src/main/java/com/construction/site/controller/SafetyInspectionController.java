package com.construction.site.controller;

import com.construction.site.entity.SafetyInspection;
import com.construction.site.service.SafetyInspectionService;
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
@RequestMapping("/api/inspections")
public class SafetyInspectionController {

    private final SafetyInspectionService service;

    public SafetyInspectionController(SafetyInspectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SafetyInspection> list(@RequestParam(required = false) Long yardId,
                                       @RequestParam(required = false) String state,
                                       @RequestParam(required = false) String keyword) {
        return service.query(yardId, state, keyword);
    }

    @PostMapping
    public SafetyInspection create(@RequestBody SafetyInspection form) {
        return service.register(form);
    }

    @PutMapping("/{id}")
    public SafetyInspection update(@PathVariable Long id, @RequestBody SafetyInspection form) {
        return service.modify(id, form);
    }
}
