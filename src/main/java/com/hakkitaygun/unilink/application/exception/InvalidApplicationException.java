package com.hakkitaygun.unilink.application.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidApplicationException extends ApiException {

    public InvalidApplicationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}