package com.hakkitaygun.unilink.posting.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidPostingException extends ApiException {

    public InvalidPostingException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}