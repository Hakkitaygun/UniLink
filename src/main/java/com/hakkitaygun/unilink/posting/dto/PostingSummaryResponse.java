package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.technology.dto.TechnologyResponse;
import com.hakkitaygun.unilink.user.entity.Profile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PostingSummaryResponse(
        UUID id,
        PostingType type,
        String title,
        String shortDescription,
        String organizationName,
        boolean open,
        boolean saved,
        Instant deadline,
        Instant createdAt,
        PostingOwnerResponse owner,
        List<PostingRoleResponse> roles,
        int totalRemaining,
        List<TechnologyResponse> technologies
) {

    private static final int SNIPPET_LENGTH = 200;

    public static PostingSummaryResponse from(Posting posting,
                                              Profile ownerProfile,
                                              Map<UUID, Integer> acceptedCounts,
                                              boolean saved) {
        List<PostingRoleResponse> roles = posting.getRoles().stream()
                .map(role -> PostingRoleResponse.from(role, acceptedCounts.getOrDefault(role.getId(), 0)))
                .sorted(Comparator.comparing(PostingRoleResponse::role, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<TechnologyResponse> technologies = posting.getTechnologies().stream()
                .map(TechnologyResponse::from)
                .sorted(Comparator.comparing(TechnologyResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int totalRemaining = roles.stream().mapToInt(PostingRoleResponse::remainingCount).sum();

        return new PostingSummaryResponse(
                posting.getId(),
                posting.getType(),
                posting.getTitle(),
                snippet(posting.getDescription()),
                posting.getOrganizationName(),
                posting.isOpen(Instant.now()),
                saved,
                posting.getDeadline(),
                posting.getCreatedAt(),
                PostingOwnerResponse.from(ownerProfile),
                roles,
                totalRemaining,
                technologies);
    }

    private static String snippet(String text) {
        if (text.length() <= SNIPPET_LENGTH) {
            return text;
        }
        return text.substring(0, SNIPPET_LENGTH).stripTrailing() + "…";
    }
}