package com.hakkitaygun.unilink.technology.repository;

import com.hakkitaygun.unilink.technology.entity.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TechnologyRepository extends JpaRepository<Technology, UUID> {

    Optional<Technology> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
