package com.hakkitaygun.unilink.posting.controller;

import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.dto.PostingSummaryResponse;
import com.hakkitaygun.unilink.posting.service.SavedPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/saved-postings")
@RequiredArgsConstructor
public class SavedPostingController {

    private final SavedPostingService savedPostingService;

    @PutMapping("/{postingId}")
    public ResponseEntity<Void> save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postingId) {
        savedPostingService.save(UUID.fromString(jwt.getSubject()), postingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postingId}")
    public ResponseEntity<Void> unsave(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID postingId) {
        savedPostingService.unsave(UUID.fromString(jwt.getSubject()), postingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public PageResponse<PostingSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "12") int size) {
        return savedPostingService.list(UUID.fromString(jwt.getSubject()), page, size);
    }
}