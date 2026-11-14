package com.hakkitaygun.unilink.security.risk;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;

import java.util.List;

public interface RiskRule {

    List<RiskHit> evaluate(SecurityEvent event);
}