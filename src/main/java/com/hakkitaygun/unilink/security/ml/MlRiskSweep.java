package com.hakkitaygun.unilink.security.ml;

import com.hakkitaygun.unilink.security.risk.RiskScoreStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class MlRiskSweep {

    private static final Logger log = LoggerFactory.getLogger(MlRiskSweep.class);

    // features.sql ile BİREBİR aynı; tek fark: tek dakika penceresi + NULLIF (boş login hash sayılmasın).
    private static final String SQL = """
            SELECT ip_address,
                   count(*)                                                                    AS req_count,
                   count(DISTINCT path_pattern)                                                AS distinct_paths,
                   coalesce(count(*) FILTER (WHERE status_code BETWEEN 400 AND 499)::float
                            / nullif(count(*), 0), 0)                                          AS err_4xx_ratio,
                   count(*) FILTER (WHERE event_type = 'ACCESS_DENIED' AND status_code = 401)  AS c401,
                   count(*) FILTER (WHERE status_code = 403)                                   AS c403,
                   count(*) FILTER (WHERE status_code = 404)                                   AS c404,
                   count(*) FILTER (WHERE event_type = 'LOGIN_FAILURE')                        AS login_fail_count,
                   count(DISTINCT NULLIF(login_identifier_hash, ''))                           AS distinct_login_ids,
                   coalesce(count(*) FILTER (WHERE http_method IN ('POST','PUT','PATCH','DELETE'))::float
                            / nullif(count(*), 0), 0)                                          AS write_ratio,
                   count(*) FILTER (WHERE path_pattern LIKE '/api/admin/%')                    AS admin_path_hits
            FROM security_events
            WHERE occurred_at >= ? AND occurred_at < ? AND status_code <> 429
            GROUP BY ip_address
            """;

    private final JdbcTemplate jdbc;
    private final MlScoringClient ml;
    private final RiskScoreStore riskStore;
    private final boolean enabled;

    private Instant lastScored;

    public MlRiskSweep(JdbcTemplate jdbc, MlScoringClient ml, RiskScoreStore riskStore,
                       @Value("${app.security.ml.enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.ml = ml;
        this.riskStore = riskStore;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.security.ml.sweep-ms:60000}",
            initialDelayString = "${app.security.ml.sweep-ms:60000}")
    public void sweep() {
        if (!enabled) return;
        Instant currentMinute = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        if (lastScored == null) lastScored = currentMinute.minus(1, ChronoUnit.MINUTES);
        int guard = 0;
        while (lastScored.isBefore(currentMinute) && guard++ < 10) {   // kaçan dakikaları telafi et (max 10)
            Instant end = lastScored.plus(1, ChronoUnit.MINUTES);
            scoreWindow(lastScored, end);
            lastScored = end;
        }
    }

    private void scoreWindow(Instant start, Instant end) {
        long now = System.currentTimeMillis();
        OffsetDateTime s = start.atOffset(ZoneOffset.UTC), e = end.atOffset(ZoneOffset.UTC);
        List<Object[]> rows = jdbc.query(SQL, (rs, i) -> new Object[]{
                rs.getString("ip_address"),
                new MlFeatures(rs.getInt("req_count"), rs.getInt("distinct_paths"), rs.getDouble("err_4xx_ratio"),
                        rs.getInt("c401"), rs.getInt("c403"), rs.getInt("c404"),
                        rs.getInt("login_fail_count"), rs.getInt("distinct_login_ids"),
                        rs.getDouble("write_ratio"), rs.getInt("admin_path_hits"))
        }, s, e);

        int flagged = 0;
        for (Object[] row : rows) {
            double mlScore = ml.score((MlFeatures) row[1]);
            if (mlScore > 0) {                                  // ml_score>0  ==  anomali (eşiğin üstünde)
                riskStore.add("ip:" + row[0], 0.4 * mlScore * 100, now);   // kural motoruyla AYNI anahtar (ip: öneki)
                flagged++;
            }
        }
        if (flagged > 0) log.info("ML tarama {}: {} istemci, {} anomali işaretlendi", s.toLocalTime(), rows.size(), flagged);
        else if (!rows.isEmpty()) log.debug("ML tarama {}: {} istemci, anomali yok", s.toLocalTime(), rows.size());
    }
}