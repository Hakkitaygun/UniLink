package com.hakkitaygun.unilink.user.repository;

import com.hakkitaygun.unilink.user.entity.Profile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    @EntityGraph(attributePaths = {"user", "technologies"})
    Optional<Profile> findWithTechnologiesByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "technologies"})
    List<Profile> findAllByUserIdIn(Collection<UUID> userIds);
}