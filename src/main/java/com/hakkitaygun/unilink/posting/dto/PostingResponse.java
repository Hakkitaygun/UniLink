package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.enums.PostingStage;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.technology.dto.TechnologyResponse;
import com.hakkitaygun.unilink.user.entity.Profile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PostingResponse(
        UUID id,
        PostingType type,
        String title,
        String description,
        String organizationName,
        PostingStage stage,
        String benefitsNote,
        PostingStatus status,
        boolean open,
        boolean saved,
        Instant deadline,
        Instant createdAt,
        PostingOwnerResponse owner,
        List<PostingRoleResponse> roles,
        List<TechnologyResponse> technologies
) {

    public static PostingResponse from(Posting posting,
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

        return new PostingResponse(
                posting.getId(),
                posting.getType(),
                posting.getTitle(),
                posting.getDescription(),
                posting.getOrganizationName(),
                posting.getStage(),
                posting.getBenefitsNote(),
                posting.getStatus(),
                posting.isOpen(Instant.now()),
                saved,
                posting.getDeadline(),
                posting.getCreatedAt(),
                PostingOwnerResponse.from(ownerProfile),
                roles,
                technologies);
    }
}