package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AccountDisabledException extends ApiException {

    public AccountDisabledException() {
        super(HttpStatus.FORBIDDEN, "Hesabınız devre dışı bırakılmış.");
    }
}
