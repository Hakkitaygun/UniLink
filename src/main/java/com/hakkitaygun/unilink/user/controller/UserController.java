package com.hakkitaygun.unilink.user.controller;

import com.hakkitaygun.unilink.user.dto.ProfileResponse;
import com.hakkitaygun.unilink.user.dto.UpdateProfileRequest;
import com.hakkitaygun.unilink.user.dto.UpdateTechnologiesRequest;
import com.hakkitaygun.unilink.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getMyProfile(UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/me")
    public ProfileResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateMyProfile(UUID.fromString(jwt.getSubject()), request);
    }

    @PutMapping("/me/technologies")
    public ProfileResponse updateMyTechnologies(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody UpdateTechnologiesRequest request) {
        return profileService.updateMyTechnologies(UUID.fromString(jwt.getSubject()), request);
    }
}