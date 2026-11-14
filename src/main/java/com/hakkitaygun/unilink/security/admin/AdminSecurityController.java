package com.hakkitaygun.unilink.security.admin;

import com.hakkitaygun.unilink.security.repository.SecurityEventRepository;
import com.hakkitaygun.unilink.security.risk.RiskScoreStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/admin/security")
public class AdminSecurityController {

    private final RiskScoreStore store;
    private final SecurityEventRepository events;

    public AdminSecurityController(RiskScoreStore store, SecurityEventRepository events) {
        this.store = store;
        this.events = events;
    }

    /** Şu an riskli/engelli istemciler: kimlik, güncel skor, band, engel bitişi (ms; 0 = engelli değil). */
    @GetMapping("/risky-clients")
    public List<RiskScoreStore.RiskSnapshot> riskyClients() {
        return store.snapshot(System.currentTimeMillis());
    }

    /** Son `hours` saatlik olay özeti: toplam, tür dağılımı, durum kodu dağılımı, en çok olan 10 IP. */
    @GetMapping("/summary")
    public SecuritySummary summary(@RequestParam(defaultValue = "24") int hours) {
        int h = Math.max(1, Math.min(hours, 720));                 // 1 saat – 30 gün
        Instant cutoff = Instant.now().minus(h, ChronoUnit.HOURS);
        return new SecuritySummary(
                h,
                events.countByOccurredAtAfter(cutoff),
                toCounts(events.countByType(cutoff)),
                toCounts(events.countByStatus(cutoff)),
                toCounts(events.topIps(cutoff, PageRequest.of(0, 10))));
    }

    private static List<Count> toCounts(List<Object[]> rows) {
        return rows.stream()
                .map(r -> new Count(String.valueOf(r[0]), ((Number) r[1]).longValue()))
                .toList();
    }

    public record Count(String key, long count) {
    }

    public record SecuritySummary(int windowHours, long totalEvents,
                                  List<Count> byType, List<Count> byStatus, List<Count> topIps) {
    }
}