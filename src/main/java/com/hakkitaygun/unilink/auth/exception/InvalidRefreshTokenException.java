package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Oturum yenilenemedi, lütfen tekrar giriş yapın.");
    }
}
