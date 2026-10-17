package com.hakkitaygun.unilink.auth.service;

import com.hakkitaygun.unilink.auth.entity.RefreshToken;
import com.hakkitaygun.unilink.auth.exception.InvalidRefreshTokenException;
import com.hakkitaygun.unilink.auth.repository.RefreshTokenRepository;
import com.hakkitaygun.unilink.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenGenerator tokenGenerator;

    @Value("${app.jwt.refresh-token-days}")
    private long refreshTokenDays;

    @Transactional
    public String create(User user) {
        String rawToken = tokenGenerator.generate();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenGenerator.hash(rawToken));
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public User consume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();
        if (token.isRevoked() || token.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        token.setRevokedAt(now);
        return token.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }
}
