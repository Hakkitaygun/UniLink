package com.hakkitaygun.unilink.user.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends ApiException {

    public ProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Profil bulunamadı.");
    }
}