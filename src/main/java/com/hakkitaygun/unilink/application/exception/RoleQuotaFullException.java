package com.hakkitaygun.unilink.application.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class RoleQuotaFullException extends ApiException {

    public RoleQuotaFullException() {
        super(HttpStatus.CONFLICT, "Bu rolün kontenjanı dolu.");
    }
}