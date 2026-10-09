package com.hakkitaygun.unilink.posting.repository;

import com.hakkitaygun.unilink.posting.entity.PostingRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostingRoleRepository extends JpaRepository<PostingRole, UUID> {

    List<PostingRole> findByPostingId(UUID postingId);
}
