package com.hakkitaygun.unilink.posting.repository;

import com.hakkitaygun.unilink.posting.entity.SavedPosting;
import com.hakkitaygun.unilink.posting.entity.SavedPostingId;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SavedPostingRepository extends JpaRepository<SavedPosting, SavedPostingId> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into saved_postings (user_id, posting_id, saved_at)
            values (:userId, :postingId, now())
            on conflict do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("postingId") UUID postingId);

    @Query("""
            select s.id.postingId from SavedPosting s
            where s.id.userId = :userId and s.id.postingId in :postingIds
            """)
    List<UUID> findSavedPostingIds(@Param("userId") UUID userId,
                                   @Param("postingIds") Collection<UUID> postingIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SavedPosting s where s.id.userId = :userId and s.id.postingId = :postingId")
    int deleteByUserAndPosting(@Param("userId") UUID userId, @Param("postingId") UUID postingId);

    @Query(value = """
            select s from SavedPosting s
            join fetch s.posting p
            where s.id.userId = :userId and p.status <> :excluded
            order by s.savedAt desc, p.id asc
            """,
            countQuery = """
            select count(s) from SavedPosting s
            where s.id.userId = :userId and s.posting.status <> :excluded
            """)
    Page<SavedPosting> findVisibleByUser(@Param("userId") UUID userId,
                                         @Param("excluded") PostingStatus excluded,
                                         Pageable pageable);
}