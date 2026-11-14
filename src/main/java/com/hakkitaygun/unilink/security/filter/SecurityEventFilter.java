package com.hakkitaygun.unilink.security.filter;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;
import com.hakkitaygun.unilink.security.enums.SecurityEventType;
import com.hakkitaygun.unilink.security.risk.RiskEngine;
import com.hakkitaygun.unilink.security.service.SecurityEventService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SecurityEventFilter extends OncePerRequestFilter {

    private static final String BEST_PATTERN_ATTRIBUTE =
            "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";
    private static final Pattern UUID_SEGMENT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final SecurityEventService securityEventService;
    private final JwtDecoder jwtDecoder;

    private final RiskEngine riskEngine;

    @Value("${app.security.events.enabled:true}")
    private boolean enabled;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                record(request, response, start);
            } catch (Exception ex) {
                log.debug("Güvenlik olayı hazırlanamadı: {}", ex.getMessage());
            }
        }
    }

    private void record(HttpServletRequest request, HttpServletResponse response, long start) {
        String method = request.getMethod();
        String rawPath = request.getRequestURI();
        if ("OPTIONS".equals(method) || isNoise(rawPath)) {
            return;
        }

        int status = response.getStatus();
        String pathPattern = resolvePathPattern(request, rawPath);

        SecurityEvent event = SecurityEvent.builder()
                .occurredAt(Instant.now())
                .eventType(classify(method, pathPattern, status))
                .userId(resolveUserId(request))
                .ipAddress(clientIp(request))
                .httpMethod(method)
                .pathPattern(pathPattern)
                .statusCode(status)
                .durationMs((int) ((System.nanoTime() - start) / 1_000_000))
                .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
                .build();

        try {
            riskEngine.onEvent(event);   // bellekte, eşzamanlı: kuyruk dolsa bile puanlama kör kalmaz
        } catch (Exception ex) {
            log.debug("Risk değerlendirmesi başarısız: {}", ex.getMessage());
        }
        securityEventService.record(event);   // veritabanı yazımı arka planda
    }

    private SecurityEventType classify(String method, String pathPattern, int status) {
        if ("POST".equals(method) && "/api/auth/login".equals(pathPattern)) {
            if (status == 200) {
                return SecurityEventType.LOGIN_SUCCESS;
            }
            if (status == 401) {
                return SecurityEventType.LOGIN_FAILURE;
            }
        }
        if (status == 401 || status == 403) {
            return SecurityEventType.ACCESS_DENIED;
        }
        return SecurityEventType.REQUEST;
    }

    private boolean isNoise(String path) {
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.equals("/favicon.ico");
    }

    /** Eşleşen Spring şablonu ("/api/postings/{id}"); eşleşme yoksa kimlikleri maskelenmiş ham yol. */
    private static final Set<String> KNOWN_RESOURCES = Set.of(
            "auth", "users", "postings", "applications", "saved-postings", "technologies");
    private static final Set<String> AUTH_ENDPOINTS = Set.of(
            "login", "logout", "refresh", "register", "verify-email",
            "forgot-password", "reset-password", "resend-verification");

    /** Eşleşen Spring şablonu; eşleşme yoksa sınırlı sayıda değer üreten kaba yol. */
    private String resolvePathPattern(HttpServletRequest request, String rawPath) {
        Object matched = request.getAttribute(BEST_PATTERN_ATTRIBUTE);
        if (matched instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        return coarsePath(rawPath);
    }

    private String coarsePath(String rawPath) {
        String[] seg = rawPath.split("/");          // "/api/postings/x" -> ["", "api", "postings", "x"]
        if (seg.length < 3 || !"api".equals(seg[1])) {
            return "/**";
        }
        String resource = seg[2];
        if ("admin".equals(resource)) {
            return "/api/admin/**";
        }
        if (!KNOWN_RESOURCES.contains(resource)) {
            return "/api/**";
        }
        if (seg.length == 3) {
            return "/api/" + resource;
        }
        if ("auth".equals(resource) && seg.length == 4 && AUTH_ENDPOINTS.contains(seg[3])) {
            return "/api/auth/" + seg[3];
        }
        return "/api/" + resource + "/**";
    }

    private UUID resolveUserId(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            Jwt jwt = jwtDecoder.decode(header.substring(7));
            return UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}