package com.construction.site.controller;

import com.construction.site.entity.Yard;
import com.construction.site.service.YardService;
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
@RequestMapping("/api/yards")
public class YardController {

    private final YardService service;

    public YardController(YardService service) {
        this.service = service;
    }

    @GetMapping
    public List<Yard> list(@RequestParam(required = false) String state,
                           @RequestParam(required = false) String keyword) {
        return service.query(state, keyword);
    }

    @PostMapping
    public Yard create(@RequestBody Yard form) {
        return service.register(form);
    }

    @PutMapping("/{id}")
    public Yard update(@PathVariable Long id, @RequestBody Yard form) {
        return service.modify(id, form);
    }
}
