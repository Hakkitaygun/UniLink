package com.hakkitaygun.unilink.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AcceptApplicationRequest(
        @NotNull
        UUID roleId
) {
}