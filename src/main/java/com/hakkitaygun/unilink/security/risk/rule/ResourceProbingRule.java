package com.hakkitaygun.unilink.security.risk.rule;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;
import com.hakkitaygun.unilink.security.risk.RiskHit;
import com.hakkitaygun.unilink.security.risk.RiskIdentity;
import com.hakkitaygun.unilink.security.risk.RiskRule;
import com.hakkitaygun.unilink.security.risk.SlidingWindowCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kimlik içeren yollara kısa sürede çok sayıda 403/404: başkasının başvurusunu/ilanını
 * kimlik deneyerek bulmaya çalışmak (yetkisiz nesne erişimi, IDOR).
 */
@Component
@RequiredArgsConstructor
public class ResourceProbingRule implements RiskRule {

    private final SlidingWindowCounter counter;

    @Value("${app.security.risk.resource-probing.threshold:5}")
    private int threshold;

    @Value("${app.security.risk.resource-probing.window-seconds:60}")
    private long windowSeconds;

    @Value("${app.security.risk.resource-probing.points:30}")
    private int points;

    @Override
    public List<RiskHit> evaluate(SecurityEvent event) {
        int status = event.getStatusCode();
        if (status != 403 && status != 404) {
            return List.of();
        }
        String path = event.getPathPattern();
        // Sadece kimlik değişkeni olan yollar; giriş uçları ve admin yolları başka kurala ait
        if (!path.contains("{") || path.startsWith("/api/auth/") || AdminProbeRule.isAdminPath(path)) {
            return List.of();
        }
        String actor = RiskIdentity.actor(event);
        int count = counter.hit("probe:" + actor, event.getOccurredAt().toEpochMilli(), windowSeconds * 1000);
        if (count % threshold == 0) {
            return List.of(new RiskHit(actor, points, "RESOURCE_PROBING"));
        }
        return List.of();
    }
}