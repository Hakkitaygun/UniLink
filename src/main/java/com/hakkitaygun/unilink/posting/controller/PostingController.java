package com.hakkitaygun.unilink.posting.controller;

import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.dto.*;
import com.hakkitaygun.unilink.posting.enums.PostingSort;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.posting.service.PostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping
    public PageResponse<PostingSummaryResponse> feed(@AuthenticationPrincipal Jwt jwt,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "12") int size,
                                                     @RequestParam(defaultValue = "NEWEST") PostingSort sort,
                                                     @RequestParam(required = false) PostingType type,
                                                     @RequestParam(required = false) String query,
                                                     @RequestParam(required = false) List<String> technologies,
                                                     @RequestParam(required = false) String role,
                                                     @RequestParam(required = false) String department,
                                                     @RequestParam(required = false) String university,
                                                     @RequestParam(required = false) Boolean hasVacancy) {
        PostingFilter filter = new PostingFilter(type, query, technologies, role, department, university, hasVacancy);
        return postingService.getFeed(page, size, sort, filter, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public PostingResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return postingService.getById(id, UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public PostingResponse update(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable UUID id,
                                  @Valid @RequestBody UpdatePostingRequest request) {
        return postingService.update(id, UUID.fromString(jwt.getSubject()), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        postingService.cancel(id, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}