package com.hakkitaygun.unilink.user.service;

import com.hakkitaygun.unilink.technology.entity.Technology;
import com.hakkitaygun.unilink.technology.service.TechnologyService;
import com.hakkitaygun.unilink.user.dto.ProfileResponse;
import com.hakkitaygun.unilink.user.dto.UpdateProfileRequest;
import com.hakkitaygun.unilink.user.dto.UpdateTechnologiesRequest;
import com.hakkitaygun.unilink.user.entity.Profile;
import com.hakkitaygun.unilink.user.exception.ProfileNotFoundException;
import com.hakkitaygun.unilink.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final TechnologyService technologyService;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID userId) {
        Profile profile = profileRepository.findWithTechnologiesByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findWithTechnologiesByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);

        profile.setFullName(request.fullName().trim());
        profile.setUniversity(request.university().trim());
        profile.setDepartment(request.department().trim());
        profile.setBio(blankToNull(request.bio()));
        profile.setGithubUrl(blankToNull(request.githubUrl()));
        profile.setLinkedinUrl(blankToNull(request.linkedinUrl()));

        return ProfileResponse.from(profile);
    }


    @Transactional
    public ProfileResponse updateMyTechnologies(UUID userId, UpdateTechnologiesRequest request) {
        Profile profile = profileRepository.findWithTechnologiesByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);

        Set<Technology> technologies = technologyService.resolveOrCreate(request.technologies());

        profile.getTechnologies().clear();
        profile.getTechnologies().addAll(technologies);

        return ProfileResponse.from(profile);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}