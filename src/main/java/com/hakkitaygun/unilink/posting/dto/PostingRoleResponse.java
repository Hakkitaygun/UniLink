package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.posting.entity.PostingRole;

import java.util.UUID;

public record PostingRoleResponse(
        UUID id,
        String role,
        int requiredCount,
        int acceptedCount,
        int remainingCount
) {

    public static PostingRoleResponse from(PostingRole role, int acceptedCount) {
        int remaining = Math.max(role.getRequiredCount() - acceptedCount, 0);
        return new PostingRoleResponse(
                role.getId(),
                role.getRole(),
                role.getRequiredCount(),
                acceptedCount,
                remaining);
    }
}