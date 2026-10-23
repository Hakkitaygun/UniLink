package com.hakkitaygun.unilink.posting.controller;

import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.dto.MyPostingResponse;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.service.MyPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/postings")
@RequiredArgsConstructor
public class MyPostingController {

    private final MyPostingService myPostingService;

    @GetMapping
    public PageResponse<MyPostingResponse> list(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam(required = false) PostingStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "12") int size) {
        return myPostingService.list(UUID.fromString(jwt.getSubject()), status, page, size);
    }
}