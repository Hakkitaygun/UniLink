package com.hakkitaygun.unilink.security.risk;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEngine {

    private final List<RiskRule> rules;
    private final RiskScoreStore store;

    public void onEvent(SecurityEvent event) {
        long now = event.getOccurredAt().toEpochMilli();
        for (RiskRule rule : rules) {
            for (RiskHit hit : rule.evaluate(event)) {
                double after = store.add(hit.identity(), hit.points(), now);
                log.info("Risk kuralı {} tetiklendi: {} +{} -> {} ({})",
                        hit.rule(), hit.identity(), hit.points(), Math.round(after), RiskBand.of(after));
            }
        }
    }
}