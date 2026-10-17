package com.hakkitaygun.unilink.auth.repository;

import com.hakkitaygun.unilink.auth.entity.VerificationToken;
import com.hakkitaygun.unilink.auth.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    Optional<VerificationToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, TokenType type);

    void deleteByUserIdAndType(UUID userId, TokenType type);
}
