package com.hakkitaygun.unilink.security.service;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;
import com.hakkitaygun.unilink.security.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    @Async("securityEventExecutor")
    public void record(SecurityEvent event) {
        try {
            securityEventRepository.save(event);
        } catch (Exception ex) {
            log.warn("Güvenlik olayı kaydedilemedi: {}", ex.getMessage());
        }
    }

    @Transactional
    public int deleteOlderThan(Instant cutoff) {
        return securityEventRepository.deleteOlderThan(cutoff);
    }
}