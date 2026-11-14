package com.hakkitaygun.unilink.security.enums;

public enum SecurityEventType {
    REQUEST,        // sıradan istek
    LOGIN_SUCCESS,  // başarılı giriş
    LOGIN_FAILURE,  // hatalı şifre / bilinmeyen kullanıcı
    ACCESS_DENIED   // 401 / 403 ile reddedilen istek (giriş dışı)
}