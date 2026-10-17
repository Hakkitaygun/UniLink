package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "Bu e-posta adresi zaten kayıtlı.");
    }
}
