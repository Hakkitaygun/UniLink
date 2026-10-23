package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends ApiException {

    public EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, "E-posta adresiniz henüz doğrulanmadı.");
    }
}
