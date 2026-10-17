package com.hakkitaygun.unilink.posting.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostingRoleRequest(
        @NotBlank
        @Size(max = 50)
        String role,

        @NotNull
        @Min(value = 1, message = "Kişi sayısı en az 1 olmalıdır.")
        @Max(value = 50, message = "Kişi sayısı en fazla 50 olabilir.")
        Integer requiredCount
) {

    public PostingRoleRequest {
        role = role == null ? null : role.trim().replaceAll("\\s+", " ");
    }
}