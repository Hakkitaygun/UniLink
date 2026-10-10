package com.hakkitaygun.unilink.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String email,
        boolean emailVerified
) {
}
