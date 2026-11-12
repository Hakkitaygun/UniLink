package com.hakkitaygun.unilink.security.scheduler;

import com.hakkitaygun.unilink.security.service.SecurityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityEventCleanupScheduler {

    private final SecurityEventService securityEventService;

    @Value("${app.security.events.retention-days:30}")
    private long retentionDays;

    @Scheduled(fixedDelayString = "${app.security.events.cleanup-ms:3600000}", initialDelay = 60000)
    public void cleanup() {
        int deleted = securityEventService.deleteOlderThan(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Saklama süresi dolan {} güvenlik olayı silindi.", deleted);
        }
    }
}