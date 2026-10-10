package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidResetTokenException extends ApiException {

    public InvalidResetTokenException() {
        super(HttpStatus.BAD_REQUEST, "Şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş.");
    }
}