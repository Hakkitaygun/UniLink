package com.hakkitaygun.unilink.technology.controller;

import com.hakkitaygun.unilink.technology.dto.TechnologyResponse;
import com.hakkitaygun.unilink.technology.service.TechnologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/technologies")
@RequiredArgsConstructor
public class TechnologyController {

    private final TechnologyService technologyService;

    @GetMapping
    public List<TechnologyResponse> search(@RequestParam(required = false) String query) {
        return technologyService.search(query);
    }
}