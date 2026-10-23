package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.posting.enums.PostingType;

import java.util.List;

public record PostingFilter(
        PostingType type,
        String query,
        List<String> technologies,
        String role,
        String department,
        String university,
        Boolean hasVacancy
) {
}