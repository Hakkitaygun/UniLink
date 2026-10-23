package com.hakkitaygun.unilink.application.controller;

import com.hakkitaygun.unilink.application.dto.*;
import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.application.service.ApplicationService;
import com.hakkitaygun.unilink.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/postings/{postingId}/applications")
    public ResponseEntity<ApplicationResponse> apply(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable UUID postingId,
                                                     @Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService
                .apply(postingId, UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/postings/{postingId}/applications")
    public PageResponse<IncomingApplicationResponse> forPosting(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable UUID postingId,
                                                                @RequestParam(required = false) ApplicationStatus status,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return applicationService.getForPosting(postingId, UUID.fromString(jwt.getSubject()), status, page, size);
    }

    @GetMapping("/applications/incoming/pending-count")
    public PendingCountResponse pendingCount(@AuthenticationPrincipal Jwt jwt) {
        return applicationService.countPending(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/applications/{applicationId}/accept")
    public IncomingApplicationResponse accept(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID applicationId,
                                              @Valid @RequestBody AcceptApplicationRequest request) {
        return applicationService.accept(applicationId, UUID.fromString(jwt.getSubject()), request.roleId());
    }

    @PostMapping("/applications/{applicationId}/reject")
    public IncomingApplicationResponse reject(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable UUID applicationId) {
        return applicationService.reject(applicationId, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    public ApplicationResponse withdraw(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID applicationId) {
        return applicationService.withdraw(applicationId, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/applications/mine")
    public PageResponse<ApplicationResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return applicationService.getMine(UUID.fromString(jwt.getSubject()), page, size);
    }
}