package com.hakkitaygun.unilink.auth.service;

import com.hakkitaygun.unilink.auth.dto.*;
import com.hakkitaygun.unilink.auth.entity.VerificationToken;
import com.hakkitaygun.unilink.auth.enums.TokenType;
import com.hakkitaygun.unilink.auth.exception.*;
import com.hakkitaygun.unilink.auth.repository.VerificationTokenRepository;
import com.hakkitaygun.unilink.user.entity.Profile;
import com.hakkitaygun.unilink.user.entity.User;
import com.hakkitaygun.unilink.user.repository.ProfileRepository;
import com.hakkitaygun.unilink.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenGenerator tokenGenerator;


    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (email == null || !email.endsWith(".edu.tr")) {
            throw new EduEmailRequiredException();
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        user = userRepository.save(user);

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.fullName())
                .university(request.university())
                .department(request.department())
                .build();
        profileRepository.save(profile);

        sendVerificationEmail(user);
        return new RegisterResponse(user.getId(), user.getEmail(), user.isEmailVerified());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidVerificationTokenException();
        }

        VerificationToken token = verificationTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .filter(savedToken -> savedToken.getType() == TokenType.EMAIL_VERIFICATION)
                .orElseThrow(InvalidVerificationTokenException::new);

        Instant now = Instant.now();
        if (token.isUsed() || token.isExpired(now)) {
            throw new InvalidVerificationTokenException();
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        token.setUsedAt(now);
        userRepository.save(user);
        verificationTokenRepository.save(token);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = User.normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.isBanned() || user.getDeletedAt() != null) {
            throw new AccountDisabledException();
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds());
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.consume(request.refreshToken());

        if (user.isBanned() || user.getDeletedAt() != null) {
            throw new AccountDisabledException();
        }

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.create(user);
        return new LoginResponse(accessToken, newRefreshToken, "Bearer", jwtService.getAccessTokenSeconds());
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        String email = User.normalizeEmail(request.email());

        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .filter(user -> !user.isBanned() && user.getDeletedAt() == null)
                .ifPresent(user -> {
                    if (sentRecently(user, TokenType.EMAIL_VERIFICATION)) {
                        return;
                    }
                    verificationTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.EMAIL_VERIFICATION);
                    sendVerificationEmail(user);
                });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = User.normalizeEmail(request.email());

        userRepository.findByEmail(email)
                .filter(user -> !user.isBanned() && user.getDeletedAt() == null)
                .ifPresent(user -> {
                    if (sentRecently(user, TokenType.PASSWORD_RESET)) {
                        return;
                    }
                    verificationTokenRepository.deleteByUserIdAndType(user.getId(), TokenType.PASSWORD_RESET);
                    sendPasswordResetEmail(user);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken token = verificationTokenRepository.findByTokenHash(tokenGenerator.hash(request.token()))
                .filter(savedToken -> savedToken.getType() == TokenType.PASSWORD_RESET)
                .orElseThrow(InvalidResetTokenException::new);

        Instant now = Instant.now();
        if (token.isUsed() || token.isExpired(now)) {
            throw new InvalidResetTokenException();
        }

        User user = token.getUser();
        if (user.isBanned() || user.getDeletedAt() != null) {
            throw new AccountDisabledException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);
        refreshTokenService.revokeAll(user.getId());
    }

    private void sendPasswordResetEmail(User user) {
        String rawToken = tokenGenerator.generate();

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenGenerator.hash(rawToken));
        token.setType(TokenType.PASSWORD_RESET);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        verificationTokenRepository.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
    }

    private boolean sentRecently(User user, TokenType type) {
        return verificationTokenRepository
                .findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), type)
                .map(token -> token.getCreatedAt().isAfter(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .orElse(false);
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private void sendVerificationEmail(User user) {
        String rawToken = tokenGenerator.generate();

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenGenerator.hash(rawToken));
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        verificationTokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), rawToken);
    }
}