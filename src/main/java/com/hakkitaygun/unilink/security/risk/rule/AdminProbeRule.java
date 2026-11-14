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
import java.util.regex.Pattern;

/** Yönetim ve hassas adreslerin yoklanması (tarayıcı botlar, yetkisiz kullanıcılar). */
@Component
@RequiredArgsConstructor
public class AdminProbeRule implements RiskRule {

    private static final Pattern ADMIN_PATH = Pattern.compile(
            "(?i)^/(api/)?(admin|actuator|manage|console|wp-admin|phpmyadmin|\\.env|\\.git|server-status).*");

    private final SlidingWindowCounter counter;

    @Value("${app.security.risk.admin-probe.threshold:3}")
    private int threshold;

    @Value("${app.security.risk.admin-probe.window-seconds:600}")
    private long windowSeconds;

    @Value("${app.security.risk.admin-probe.points:40}")
    private int points;

    public static boolean isAdminPath(String path) {
        return ADMIN_PATH.matcher(path).matches();
    }

    @Override
    public List<RiskHit> evaluate(SecurityEvent event) {
        int status = event.getStatusCode();
        if ((status != 401 && status != 403 && status != 404) || !isAdminPath(event.getPathPattern())) {
            return List.of();
        }
        String actor = RiskIdentity.actor(event);
        int count = counter.hit("admin-probe:" + actor, event.getOccurredAt().toEpochMilli(), windowSeconds * 1000);
        if (count % threshold == 0) {
            return List.of(new RiskHit(actor, points, "ADMIN_PROBE"));
        }
        return List.of();
    }
}