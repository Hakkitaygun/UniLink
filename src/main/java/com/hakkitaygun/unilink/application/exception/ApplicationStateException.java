package com.hakkitaygun.unilink.application.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ApplicationStateException extends ApiException {

    public ApplicationStateException() {
        super(HttpStatus.CONFLICT, "Bu başvuru zaten sonuçlandırılmış.");
    }

    public ApplicationStateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}