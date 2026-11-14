package com.hakkitaygun.unilink.security.risk;

public enum RiskBand {
    LOW,       // 0-29   izin ver
    MEDIUM,    // 30-59  yavaşlat
    HIGH,      // 60-79  hassas işlemleri kısıtla
    CRITICAL;  // 80+    geçici engelle

    public static RiskBand of(double score) {
        if (score >= 80) {
            return CRITICAL;
        }
        if (score >= 60) {
            return HIGH;
        }
        if (score >= 30) {
            return MEDIUM;
        }
        return LOW;
    }
}