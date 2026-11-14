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

/** Kısa sürede aşırı istek (ilanları toplu tarama, hizmet aksatma denemesi). */
@Component
@RequiredArgsConstructor
public class HighRequestRateRule implements RiskRule {

    private final SlidingWindowCounter counter;

    @Value("${app.security.risk.rate.threshold:100}")
    private int threshold;

    @Value("${app.security.risk.rate.window-seconds:10}")
    private long windowSeconds;

    @Value("${app.security.risk.rate.points:20}")
    private int points;

    @Override
    public List<RiskHit> evaluate(SecurityEvent event) {
        String actor = RiskIdentity.actor(event);
        int count = counter.hit("rate:" + actor, event.getOccurredAt().toEpochMilli(), windowSeconds * 1000);
        if (count % threshold == 0) {
            return List.of(new RiskHit(actor, points, "HIGH_REQUEST_RATE"));
        }
        return List.of();
    }
}