package com.hakkitaygun.unilink.posting.repository;

import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PostingRepository extends JpaRepository<Posting, UUID>, JpaSpecificationExecutor<Posting> {

    @EntityGraph(attributePaths = {"roles", "technologies"})
    Optional<Posting> findWithDetailsById(UUID id);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Posting p set p.status = :to where p.status = :from and p.deadline <= :now")
    int updateStatusWhenExpired(@Param("from") PostingStatus from,
                                @Param("to") PostingStatus to,
                                @Param("now") Instant now);
}