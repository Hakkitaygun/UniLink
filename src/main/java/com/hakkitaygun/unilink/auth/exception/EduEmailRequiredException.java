package com.hakkitaygun.unilink.auth.exception;

import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EduEmailRequiredException extends ApiException {

    public EduEmailRequiredException() {
        super(HttpStatus.BAD_REQUEST, "Kayıt için .edu.tr uzantılı bir e-posta adresi gereklidir.");
    }
}
