package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends ApiException {

    public InvalidVerificationTokenException() {
        super(HttpStatus.BAD_REQUEST, "Doğrulama bağlantısı geçersiz veya süresi dolmuş.");
    }
}
