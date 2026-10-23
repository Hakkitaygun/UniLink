package com.hakkitaygun.unilink.application.repository;

import com.hakkitaygun.unilink.application.entity.Application;
import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByPostingIdAndApplicantId(UUID postingId, UUID applicantId);

    Optional<Application> findByPostingIdAndApplicantId(UUID postingId, UUID applicantId);

    @EntityGraph(attributePaths = {"posting", "acceptedRole"})
    Page<Application> findByApplicantId(UUID applicantId, Pageable pageable);

    @EntityGraph(attributePaths = "acceptedRole")
    Page<Application> findByPostingId(UUID postingId, Pageable pageable);

    @EntityGraph(attributePaths = "acceptedRole")
    Page<Application> findByPostingIdAndStatus(UUID postingId, ApplicationStatus status, Pageable pageable);

    @Query("""
            select count(a) from Application a
            where a.posting.owner.id = :ownerId
              and a.status = :status
              and a.posting.status <> :excludedPostingStatus
            """)
    long countByOwnerAndStatus(@Param("ownerId") UUID ownerId,
                               @Param("status") ApplicationStatus status,
                               @Param("excludedPostingStatus") PostingStatus excludedPostingStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") UUID id);

    long countByAcceptedRoleIdAndStatus(UUID roleId, ApplicationStatus status);

    @Query("""
            select a.posting.id as postingId, a.status as status, count(a) as total
            from Application a
            where a.posting.id in :postingIds
            group by a.posting.id, a.status
            """)
    List<PostingApplicationCount> countByPostingIds(@Param("postingIds") Collection<UUID> postingIds);

    interface PostingApplicationCount {

        UUID getPostingId();

        ApplicationStatus getStatus();

        long getTotal();
    }
}