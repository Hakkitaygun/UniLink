package com.hakkitaygun.unilink.security.risk;

public record RiskHit(String identity, int points, String rule) {
}