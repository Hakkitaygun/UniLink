package com.hakkitaygun.unilink.user.service;

import com.hakkitaygun.unilink.auth.dto.LoginResponse;
import com.hakkitaygun.unilink.auth.service.JwtService;
import com.hakkitaygun.unilink.auth.service.RefreshTokenService;
import com.hakkitaygun.unilink.user.dto.ChangePasswordRequest;
import com.hakkitaygun.unilink.user.entity.User;
import com.hakkitaygun.unilink.user.exception.PasswordChangeException;
import com.hakkitaygun.unilink.user.exception.ProfileNotFoundException;
import com.hakkitaygun.unilink.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(ProfileNotFoundException::new);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new PasswordChangeException("Mevcut şifre hatalı.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new PasswordChangeException("Yeni şifre mevcut şifreyle aynı olamaz.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(userId);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds());
    }
}