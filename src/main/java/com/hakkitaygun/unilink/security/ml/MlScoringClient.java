package com.hakkitaygun.unilink.security.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MlScoringClient {

    private static final Logger log = LoggerFactory.getLogger(MlScoringClient.class);

    private final boolean enabled;
    private final RestClient client;
    private final AtomicLong lastWarn = new AtomicLong();

    public MlScoringClient(
            @Value("${app.security.ml.enabled:false}") boolean enabled,
            @Value("${app.security.ml.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${app.security.ml.timeout-ms:800}") long timeoutMs) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofMillis(timeoutMs));
        rf.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(rf).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** ml_score [0,1]; servis kapalı veya erişilemezse 0.0 (yalnızca kural skoru = fallback). */
    public double score(MlFeatures f) {
        if (!enabled) return 0.0;
        try {
            Map<String, Object> body = Map.ofEntries(
                    Map.entry("req_count", f.reqCount()),
                    Map.entry("distinct_paths", f.distinctPaths()),
                    Map.entry("err_4xx_ratio", f.err4xxRatio()),
                    Map.entry("c401", f.c401()),
                    Map.entry("c403", f.c403()),
                    Map.entry("c404", f.c404()),
                    Map.entry("login_fail_count", f.loginFailCount()),
                    Map.entry("distinct_login_ids", f.distinctLoginIds()),
                    Map.entry("write_ratio", f.writeRatio()),
                    Map.entry("admin_path_hits", f.adminPathHits()));
            MlResponse r = client.post().uri("/score")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(MlResponse.class);
            return r == null ? 0.0 : Math.max(0.0, Math.min(1.0, r.mlScore()));
        } catch (Exception e) {
            warnThrottled(e);
            return 0.0;   // servis yoksa sistem kural skoruyla çalışmaya devam eder
        }
    }

    private void warnThrottled(Exception e) {
        long now = System.currentTimeMillis(), prev = lastWarn.get();
        if (now - prev > 30_000 && lastWarn.compareAndSet(prev, now)) {
            log.warn("ML servisi erişilemedi, yalnızca kural skoru kullanılacak: {}", e.toString());
        }
    }

    private record MlResponse(@JsonProperty("ml_score") double mlScore,
                              @JsonProperty("is_anomaly") boolean isAnomaly) {}
}