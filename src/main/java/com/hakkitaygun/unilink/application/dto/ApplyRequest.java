package com.hakkitaygun.unilink.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record ApplyRequest(
        @Size(max = 1000)
        String message,

        @NotEmpty(message = "En az bir rol seçmelisiniz.")
        @Size(max = 5, message = "En fazla 5 role başvurabilirsiniz.")
        Set<@NotNull UUID> roleIds
) {

    public ApplyRequest {
        message = message == null || message.isBlank() ? null : message.trim();
    }
}