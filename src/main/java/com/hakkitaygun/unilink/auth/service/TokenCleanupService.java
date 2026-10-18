package com.hakkitaygun.unilink.auth.service;

import com.hakkitaygun.unilink.auth.repository.RefreshTokenRepository;
import com.hakkitaygun.unilink.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public record CleanupResult(int verificationTokens, int refreshTokens) {
    }

    @Transactional
    public CleanupResult deleteExpired() {
        Instant now = Instant.now();
        int verification = verificationTokenRepository.deleteExpired(now);
        int refresh = refreshTokenRepository.deleteExpired(now);
        return new CleanupResult(verification, refresh);
    }
}