package com.hakkitaygun.unilink.application.dto;

import com.hakkitaygun.unilink.application.entity.Application;
import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.enums.PostingType;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        ApplicationStatus status,
        String message,
        Instant createdAt,
        Instant decidedAt,
        PostingInfo posting,
        List<RoleInfo> appliedRoles,
        RoleInfo acceptedRole
) {

    public record PostingInfo(
            UUID id,
            String title,
            PostingType type,
            PostingStatus status,
            boolean open,
            Instant deadline
    ) {
    }

    public record RoleInfo(UUID id, String role) {

        static RoleInfo from(PostingRole role) {
            return new RoleInfo(role.getId(), role.getRole());
        }
    }

    public static ApplicationResponse from(Application application) {
        Posting posting = application.getPosting();

        List<RoleInfo> appliedRoles = application.getAppliedRoles().stream()
                .map(RoleInfo::from)
                .sorted(Comparator.comparing(RoleInfo::role, String.CASE_INSENSITIVE_ORDER))
                .toList();

        RoleInfo acceptedRole = application.getAcceptedRole() == null
                ? null
                : RoleInfo.from(application.getAcceptedRole());

        return new ApplicationResponse(
                application.getId(),
                application.getStatus(),
                application.getMessage(),
                application.getCreatedAt(),
                application.getDecidedAt(),
                new PostingInfo(
                        posting.getId(),
                        posting.getTitle(),
                        posting.getType(),
                        posting.getStatus(),
                        posting.isOpen(Instant.now()),
                        posting.getDeadline()),
                appliedRoles,
                acceptedRole);
    }
}