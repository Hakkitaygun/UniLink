package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.user.entity.Profile;

import java.util.UUID;

public record PostingOwnerResponse(
        UUID id,
        String fullName,
        String university,
        String department
) {

    public static PostingOwnerResponse from(Profile profile) {
        return new PostingOwnerResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getUniversity(),
                profile.getDepartment());
    }
}