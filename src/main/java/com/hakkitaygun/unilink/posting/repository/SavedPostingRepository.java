package com.hakkitaygun.unilink.posting.repository;

import com.hakkitaygun.unilink.posting.SavedPostingId;
import com.hakkitaygun.unilink.posting.entity.SavedPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SavedPostingRepository extends JpaRepository<SavedPosting, SavedPostingId> {

    boolean existsByUserIdAndPostingId(UUID userId, UUID postingId);

    void deleteByUserIdAndPostingId(UUID userId, UUID postingId);
}
