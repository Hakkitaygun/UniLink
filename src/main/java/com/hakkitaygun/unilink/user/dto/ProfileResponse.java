package com.hakkitaygun.unilink.user.dto;

import com.hakkitaygun.unilink.technology.dto.TechnologyResponse;
import com.hakkitaygun.unilink.user.entity.Profile;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String university,
        String department,
        String bio,
        String githubUrl,
        String linkedinUrl,
        List<TechnologyResponse> technologies
) {

    public static ProfileResponse from(Profile profile) {
        List<TechnologyResponse> technologies = profile.getTechnologies().stream()
                .map(TechnologyResponse::from)
                .sorted(Comparator.comparing(TechnologyResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new ProfileResponse(
                profile.getUserId(),
                profile.getUser().getEmail(),
                profile.getFullName(),
                profile.getUniversity(),
                profile.getDepartment(),
                profile.getBio(),
                profile.getGithubUrl(),
                profile.getLinkedinUrl(),
                technologies);
    }
}