package com.hakkitaygun.unilink.user.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PasswordChangeException extends ApiException {

    public PasswordChangeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}