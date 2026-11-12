package com.hakkitaygun.unilink.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.UUID;

public final class ClientRequestInfo {

    private ClientRequestInfo() {
    }

    public static String clientIp(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** Geçerli bir Bearer token varsa kullanıcı kimliği, yoksa null. */
    public static UUID userId(HttpServletRequest request, JwtDecoder decoder) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            Jwt jwt = decoder.decode(header.substring(7));
            return UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            return null;
        }
    }
}