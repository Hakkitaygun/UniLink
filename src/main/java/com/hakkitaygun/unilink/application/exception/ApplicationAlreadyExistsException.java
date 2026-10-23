package com.hakkitaygun.unilink.application.exception;

import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ApplicationAlreadyExistsException extends ApiException {

    public ApplicationAlreadyExistsException(ApplicationStatus status) {
        super(HttpStatus.CONFLICT, status == ApplicationStatus.REJECTED
                ? "Başvurunuz reddedildiği için bu ilana tekrar başvuramazsınız."
                : "Bu ilana zaten başvurdunuz.");
    }
}