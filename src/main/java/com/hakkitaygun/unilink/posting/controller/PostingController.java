package com.hakkitaygun.unilink.posting.controller;

import com.hakkitaygun.unilink.posting.dto.CreatePostingRequest;
import com.hakkitaygun.unilink.posting.dto.PostingResponse;
import com.hakkitaygun.unilink.posting.service.PostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class PostingController {

    private final PostingService postingService;

    @PostMapping
    public ResponseEntity<PostingResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreatePostingRequest request) {
        PostingResponse response = postingService.create(UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public PostingResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return postingService.getById(id, UUID.fromString(jwt.getSubject()));
    }
}