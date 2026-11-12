package com.hakkitaygun.unilink.user.controller;

import com.hakkitaygun.unilink.auth.dto.LoginResponse;
import com.hakkitaygun.unilink.user.dto.ChangePasswordRequest;
import com.hakkitaygun.unilink.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/password")
    public LoginResponse changePassword(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        return accountService.changePassword(UUID.fromString(jwt.getSubject()), request);
    }
}