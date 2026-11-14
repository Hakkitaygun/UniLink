package com.hakkitaygun.unilink.security.risk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RiskScoreStore {

    private static final double BLOCK_THRESHOLD = 80;

    private record State(double score, long updatedAtMs, long blockedUntilMs) {
    }

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    @Value("${app.security.risk.half-life-minutes:10}")
    private double halfLifeMinutes;

    @Value("${app.security.risk.hold-seconds:60}")
    private long holdSeconds;

    @Value("${app.security.risk.block-minutes:15}")
    private double blockMinutes;

    /** Puan ekler (üst sınır 100) ve yeni puanı döner. 80'e ulaşınca geçici engel damgası konur. */
    public double add(String identity, double points, long nowMs) {
        return states.compute(identity, (key, old) -> {
            double current = old == null ? 0 : decay(old, nowMs);
            double updated = Math.min(100, current + points);
            long blockedUntil = old == null ? 0 : old.blockedUntilMs();
            if (updated >= BLOCK_THRESHOLD && blockedUntil <= nowMs) {
                blockedUntil = nowMs + (long) (blockMinutes * 60_000);
            }
            return new State(updated, nowMs, blockedUntil);
        }).score();
    }

    public double get(String identity, long nowMs) {
        State state = states.get(identity);
        return state == null ? 0 : decay(state, nowMs);
    }

    /** Engel bitiş zamanı (ms); engelli değilse 0. */
    public long blockedUntil(String identity, long nowMs) {
        State state = states.get(identity);
        return state == null || state.blockedUntilMs() <= nowMs ? 0 : state.blockedUntilMs();
    }

    /** Puan eşiğin altına inene kadar gereken saniye (tutma süresi dahil). */
    public long secondsUntilBelow(String identity, double threshold, long nowMs) {
        State state = states.get(identity);
        if (state == null) {
            return 0;
        }
        double score = get(identity, nowMs);
        if (score < threshold) {
            return 0;
        }
        double holdLeft = Math.max(0, holdSeconds * 1000 - (nowMs - state.updatedAtMs())) / 1000.0;
        double decayMinutes = halfLifeMinutes * (Math.log(score / threshold) / Math.log(2));
        return (long) Math.ceil(holdLeft + decayMinutes * 60) + 1;
    }

    /** Tutma süresi bitmeden azalma yok; sonrasında yarı ömürle üstel azalma. */
    private double decay(State state, long nowMs) {
        double elapsedMs = Math.max(0, nowMs - state.updatedAtMs() - holdSeconds * 1000);
        double minutes = elapsedMs / 60_000.0;
        return state.score() * Math.pow(0.5, minutes / halfLifeMinutes);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void evictLow() {
        long now = System.currentTimeMillis();
        states.entrySet().removeIf(entry ->
                entry.getValue().blockedUntilMs() <= now && decay(entry.getValue(), now) < 1.0);
    }

    /** Şu an skoru anlamlı (>=1) ya da engelli tüm istemcilerin anlık görüntüsü; yüksekten düşüğe sıralı. */
    public java.util.List<RiskSnapshot> snapshot(long nowMs) {
        java.util.List<RiskSnapshot> out = new java.util.ArrayList<>();
        states.forEach((id, st) -> {
            double score = decay(st, nowMs);
            boolean blocked = st.blockedUntilMs() > nowMs;
            if (score >= 1.0 || blocked) {
                out.add(new RiskSnapshot(id, Math.round(score * 100.0) / 100.0,
                        RiskBand.of(score).name(), blocked ? st.blockedUntilMs() : 0));
            }
        });
        out.sort((a, b) -> Double.compare(b.score(), a.score()));
        return out;
    }

    public record RiskSnapshot(String identity, double score, String band, long blockedUntilMs) {
    }
}