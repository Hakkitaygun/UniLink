package com.hakkitaygun.unilink.security.risk;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DecisionEngine {

    public enum Action { ALLOW, THROTTLE, BLOCK_WRITES, BLOCK }

    public record Decision(Action action, RiskBand band, long retryAfterSeconds, String message) {

        static Decision allow(RiskBand band) {
            return new Decision(Action.ALLOW, band, 0, null);
        }
    }

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> ALWAYS_ALLOWED = Set.of("/api/auth/logout", "/api/auth/refresh");

    private static final String MSG_THROTTLE =
            "Çok fazla istek gönderdiniz. Lütfen bir süre bekleyip tekrar deneyin.";
    private static final String MSG_BLOCK_WRITES =
            "Olağan dışı etkinlik algılandı; bu işlem geçici olarak kısıtlandı. Lütfen bir süre sonra tekrar deneyin.";
    private static final String MSG_BLOCK =
            "Olağan dışı etkinlik nedeniyle erişiminiz geçici olarak kısıtlandı. Lütfen bir süre sonra tekrar deneyin.";

    private final RiskScoreStore store;
    private final SlidingWindowCounter counter;

    @Value("${app.security.enforcement.throttle-per-minute:30}")
    private int throttlePerMinute;

    public Decision decide(String identity, String method, String path, long nowMs) {
        long blockedUntil = store.blockedUntil(identity, nowMs);
        if (blockedUntil > 0) {
            long retry = Math.max(1, (blockedUntil - nowMs + 999) / 1000);
            return new Decision(Action.BLOCK, RiskBand.CRITICAL, retry, MSG_BLOCK);
        }

        double score = store.get(identity, nowMs);
        RiskBand band = RiskBand.of(score);

        switch (band) {
            case HIGH -> {
                if (WRITE_METHODS.contains(method) && !ALWAYS_ALLOWED.contains(path)) {
                    long retry = Math.max(30, store.secondsUntilBelow(identity, 60, nowMs));
                    return new Decision(Action.BLOCK_WRITES, band, retry, MSG_BLOCK_WRITES);
                }
            }
            case MEDIUM -> {
                int count = counter.hit("throttle:" + identity, nowMs, 60_000);
                if (count > throttlePerMinute) {
                    return new Decision(Action.THROTTLE, band, 30, MSG_THROTTLE);
                }
            }
            case CRITICAL -> {
                return new Decision(Action.BLOCK, band, 60, MSG_BLOCK);
            }
            default -> {
            }
        }
        return Decision.allow(band);
    }
}