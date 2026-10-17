package com.hakkitaygun.unilink.technology.dto;

import com.hakkitaygun.unilink.technology.entity.Technology;

import java.util.UUID;

public record TechnologyResponse(
        UUID id,
        String name
) {

    public static TechnologyResponse from(Technology technology) {
        return new TechnologyResponse(technology.getId(), technology.getName());
    }
}