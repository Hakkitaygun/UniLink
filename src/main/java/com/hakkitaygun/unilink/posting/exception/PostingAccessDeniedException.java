package com.hakkitaygun.unilink.posting.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PostingAccessDeniedException extends ApiException {

    public PostingAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Bu ilan üzerinde işlem yapma yetkiniz yok.");
    }
}