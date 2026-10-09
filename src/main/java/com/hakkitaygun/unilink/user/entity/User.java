package com.hakkitaygun.unilink.user.entity;

import com.hakkitaygun.unilink.common.entity.AuditableEntity;
import com.hakkitaygun.unilink.user.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends AuditableEntity {

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean banned = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static String normalizeEmail(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public boolean isActive() {
        return emailVerified && !banned && deletedAt == null;
    }
}
