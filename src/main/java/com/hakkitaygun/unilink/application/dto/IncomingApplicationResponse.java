package com.hakkitaygun.unilink.application.dto;

import com.hakkitaygun.unilink.application.entity.Application;
import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.technology.dto.TechnologyResponse;
import com.hakkitaygun.unilink.user.entity.Profile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record IncomingApplicationResponse(
        UUID id,
        ApplicationStatus status,
        String message,
        Instant createdAt,
        Instant decidedAt,
        ApplicantInfo applicant,
        List<ApplicationResponse.RoleInfo> appliedRoles,
        ApplicationResponse.RoleInfo acceptedRole
) {

    public record ApplicantInfo(
            UUID userId,
            String fullName,
            String university,
            String department,
            String bio,
            String githubUrl,
            String linkedinUrl,
            List<TechnologyResponse> technologies,
            String email
    ) {

        static ApplicantInfo from(Profile profile, boolean includeEmail) {
            List<TechnologyResponse> technologies = profile.getTechnologies().stream()
                    .map(TechnologyResponse::from)
                    .sorted(Comparator.comparing(TechnologyResponse::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            return new ApplicantInfo(
                    profile.getUserId(),
                    profile.getFullName(),
                    profile.getUniversity(),
                    profile.getDepartment(),
                    profile.getBio(),
                    profile.getGithubUrl(),
                    profile.getLinkedinUrl(),
                    technologies,
                    includeEmail ? profile.getUser().getEmail() : null);
        }
    }

    public static IncomingApplicationResponse from(Application application, Profile profile) {
        List<ApplicationResponse.RoleInfo> appliedRoles = application.getAppliedRoles().stream()
                .map(ApplicationResponse.RoleInfo::from)
                .sorted(Comparator.comparing(ApplicationResponse.RoleInfo::role, String.CASE_INSENSITIVE_ORDER))
                .toList();

        ApplicationResponse.RoleInfo acceptedRole = application.getAcceptedRole() == null
                ? null
                : ApplicationResponse.RoleInfo.from(application.getAcceptedRole());

        boolean accepted = application.getStatus() == ApplicationStatus.ACCEPTED;

        return new IncomingApplicationResponse(
                application.getId(),
                application.getStatus(),
                application.getMessage(),
                application.getCreatedAt(),
                application.getDecidedAt(),
                ApplicantInfo.from(profile, accepted),
                appliedRoles,
                acceptedRole);
    }
}