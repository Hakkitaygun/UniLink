package com.hakkitaygun.unilink.posting.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PostingNotFoundException extends ApiException {

    public PostingNotFoundException() {
        super(HttpStatus.NOT_FOUND, "İlan bulunamadı.");
    }
}