package com.hakkitaygun.unilink.security.filter;

import com.hakkitaygun.unilink.security.risk.DecisionEngine;
import com.hakkitaygun.unilink.security.risk.DecisionEngine.Action;
import com.hakkitaygun.unilink.security.risk.DecisionEngine.Decision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class RiskEnforcementFilter extends OncePerRequestFilter {

    private final DecisionEngine decisionEngine;
    private final JwtDecoder jwtDecoder;
    private final boolean enforcementEnabled;
    private final boolean trustForwardedHeaders;
    private final Set<String> exemptIps;

    public RiskEnforcementFilter(DecisionEngine decisionEngine, JwtDecoder jwtDecoder,
                                 boolean enforcementEnabled, boolean trustForwardedHeaders,
                                 Set<String> exemptIps) {
        this.decisionEngine = decisionEngine;
        this.jwtDecoder = jwtDecoder;
        this.enforcementEnabled = enforcementEnabled;
        this.trustForwardedHeaders = trustForwardedHeaders;
        this.exemptIps = exemptIps;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Decision decision = Decision_allowOnError(request);

        if (decision.action() == Action.ALLOW) {
            chain.doFilter(request, response);
            return;
        }
        if (!enforcementEnabled) {
            log.info("Gözlem modu: {} {} için {} kararı uygulanmadı ({})",
                    request.getMethod(), request.getRequestURI(), decision.action(), decision.band());
            chain.doFilter(request, response);
            return;
        }
        reject(request, response, decision);
    }

    /** Karar üretiminde beklenmeyen bir hata olursa isteği engellemek yerine geçirir (güvenlik katmanı kullanılabilirliği bozmasın). */
    private Decision Decision_allowOnError(HttpServletRequest request) {
        try {
            String method = request.getMethod();
            if ("OPTIONS".equals(method)) {
                return new Decision(Action.ALLOW, null, 0, null);
            }
            String ip = ClientRequestInfo.clientIp(request, trustForwardedHeaders);
            if (exemptIps.contains(ip)) {
                return new Decision(Action.ALLOW, null, 0, null);
            }
            UUID userId = ClientRequestInfo.userId(request, jwtDecoder);
            String identity = userId != null ? "user:" + userId : "ip:" + ip;
            return decisionEngine.decide(identity, method, request.getRequestURI(), System.currentTimeMillis());
        } catch (Exception ex) {
            log.debug("Karar üretilemedi, istek geçirildi: {}", ex.getMessage());
            return new Decision(Action.ALLOW, null, 0, null);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, Decision decision)
            throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = "{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,"
                + "\"error\":\"Too Many Requests\",\"message\":\"" + escape(decision.message()) + "\","
                + "\"path\":\"" + escape(request.getRequestURI()) + "\"}";
        response.getWriter().write(body);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}