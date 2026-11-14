package com.hakkitaygun.unilink.security.risk.rule;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;
import com.hakkitaygun.unilink.security.enums.SecurityEventType;
import com.hakkitaygun.unilink.security.risk.RiskHit;
import com.hakkitaygun.unilink.security.risk.RiskIdentity;
import com.hakkitaygun.unilink.security.risk.RiskRule;
import com.hakkitaygun.unilink.security.risk.SlidingWindowCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Aynı IP'den kısa sürede çok sayıda hatalı giriş (kaba kuvvet / parola deneme). */
@Component
@RequiredArgsConstructor
public class FailedLoginRule implements RiskRule {

    private final SlidingWindowCounter counter;

    @Value("${app.security.risk.failed-login.threshold:5}")
    private int threshold;

    @Value("${app.security.risk.failed-login.window-seconds:600}")
    private long windowSeconds;

    @Value("${app.security.risk.failed-login.points:20}")
    private int points;

    @Override
    public List<RiskHit> evaluate(SecurityEvent event) {
        if (event.getEventType() != SecurityEventType.LOGIN_FAILURE) {
            return List.of();
        }
        int count = counter.hit("failed-login:" + RiskIdentity.ip(event),
                event.getOccurredAt().toEpochMilli(), windowSeconds * 1000);
        if (count % threshold == 0) {
            return List.of(new RiskHit(RiskIdentity.ip(event), points, "FAILED_LOGIN"));
        }
        return List.of();
    }
}