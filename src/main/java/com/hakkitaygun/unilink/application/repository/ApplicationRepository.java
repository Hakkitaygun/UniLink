package com.hakkitaygun.unilink.application.repository;

import com.hakkitaygun.unilink.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByPostingIdAndApplicantId(UUID postingId, UUID applicantId);
}
