package com.hakkitaygun.unilink.security.risk;

import com.hakkitaygun.unilink.security.entity.SecurityEvent;

/** Riskin kime yazılacağı: giriş yapmışsa kullanıcıya, yoksa IP'ye. */
public final class RiskIdentity {

    private RiskIdentity() {
    }

    public static String ip(SecurityEvent event) {
        return "ip:" + event.getIpAddress();
    }

    public static String user(SecurityEvent event) {
        return event.getUserId() == null ? null : "user:" + event.getUserId();
    }

    public static String actor(SecurityEvent event) {
        String user = user(event);
        return user != null ? user : ip(event);
    }
}