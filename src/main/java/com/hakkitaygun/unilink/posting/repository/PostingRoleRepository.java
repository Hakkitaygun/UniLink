package com.hakkitaygun.unilink.posting.repository;

import com.hakkitaygun.unilink.posting.entity.PostingRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostingRoleRepository extends JpaRepository<PostingRole, UUID> {

    List<PostingRole> findByPostingId(UUID postingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PostingRole r where r.id = :id")
    Optional<PostingRole> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select a.acceptedRole.id as roleId, count(a) as acceptedCount
            from Application a
            where a.acceptedRole.id in :roleIds
            group by a.acceptedRole.id
            """)
    List<RoleAcceptedCount> countAcceptedByRoleIds(@Param("roleIds") Collection<UUID> roleIds);

    interface RoleAcceptedCount {

        UUID getRoleId();

        long getAcceptedCount();
    }
}