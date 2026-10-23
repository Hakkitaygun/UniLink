package com.hakkitaygun.unilink.application.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ApplicationNotFoundException extends ApiException {

    public ApplicationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Başvuru bulunamadı.");
    }
}