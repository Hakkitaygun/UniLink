package com.hakkitaygun.unilink.auth.scheduler;

import com.hakkitaygun.unilink.auth.service.TokenCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenCleanupService tokenCleanupService;

    @Scheduled(fixedDelayString = "${app.scheduling.cleanup-tokens-ms:3600000}", initialDelay = 30000)
    public void cleanupExpiredTokens() {
        TokenCleanupService.CleanupResult result = tokenCleanupService.deleteExpired();
        if (result.verificationTokens() > 0 || result.refreshTokens() > 0) {
            log.info("Süresi dolan {} doğrulama ve {} yenileme token'ı silindi.",
                    result.verificationTokens(), result.refreshTokens());
        }
    }
}