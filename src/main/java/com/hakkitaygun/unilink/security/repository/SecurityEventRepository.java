package com.hakkitaygun.unilink.security.repository;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SecurityEvent e where e.occurredAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    long countByOccurredAtAfter(Instant cutoff);

    @Query("select e.eventType, count(e) from SecurityEvent e where e.occurredAt >= :cutoff group by e.eventType order by count(e) desc")
    List<Object[]> countByType(@Param("cutoff") Instant cutoff);

    @Query("select e.statusCode, count(e) from SecurityEvent e where e.occurredAt >= :cutoff group by e.statusCode order by e.statusCode")
    List<Object[]> countByStatus(@Param("cutoff") Instant cutoff);

    @Query("select e.ipAddress, count(e) from SecurityEvent e where e.occurredAt >= :cutoff group by e.ipAddress order by count(e) desc")
    List<Object[]> topIps(@Param("cutoff") Instant cutoff, Pageable pageable);
}