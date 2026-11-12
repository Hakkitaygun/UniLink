package com.hakkitaygun.unilink.security.entity;

import com.hakkitaygun.unilink.common.entity.BaseEntity;
import com.hakkitaygun.unilink.security.enums.SecurityEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityEvent extends BaseEntity {

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40, updatable = false)
    private SecurityEventType eventType;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "ip_address", nullable = false, length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "http_method", nullable = false, length = 10, updatable = false)
    private String httpMethod;

    @Column(name = "path_pattern", nullable = false, length = 200, updatable = false)
    private String pathPattern;

    @Column(name = "status_code", nullable = false, updatable = false)
    private int statusCode;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private int durationMs;

    @Column(name = "user_agent", length = 200, updatable = false)
    private String userAgent;

    @Column(name = "login_identifier_hash", length = 64, updatable = false)
    private String loginIdentifierHash;

    @Builder
    public SecurityEvent(Instant occurredAt, SecurityEventType eventType, UUID userId, String ipAddress,
                         String httpMethod, String pathPattern, int statusCode, int durationMs,
                         String userAgent, String loginIdentifierHash) {
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.userId = userId;
        this.ipAddress = truncate(ipAddress, 45);
        this.httpMethod = truncate(httpMethod, 10);
        this.pathPattern = truncate(pathPattern, 200);
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.userAgent = truncate(userAgent, 200);
        this.loginIdentifierHash = loginIdentifierHash;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}