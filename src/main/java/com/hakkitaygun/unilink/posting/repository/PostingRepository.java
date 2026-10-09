package com.hakkitaygun.unilink.posting.repository;

import com.hakkitaygun.unilink.posting.entity.Posting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostingRepository extends JpaRepository<Posting, UUID> {
}
