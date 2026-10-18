package com.hakkitaygun.unilink.auth.repository;

import com.hakkitaygun.unilink.auth.entity.VerificationToken;
import com.hakkitaygun.unilink.auth.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    Optional<VerificationToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, TokenType type);

    void deleteByUserIdAndType(UUID userId, TokenType type);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VerificationToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
