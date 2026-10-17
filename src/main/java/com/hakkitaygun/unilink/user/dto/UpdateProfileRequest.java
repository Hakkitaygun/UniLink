package com.hakkitaygun.unilink.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Size(max = 150)
        String university,

        @NotBlank
        @Size(max = 150)
        String department,

        @Size(max = 1000)
        String bio,

        @Size(max = 255)
        @Pattern(regexp = "^$|^https://(www\\.)?github\\.com/.+",
                message = "GitHub bağlantısı https://github.com/ ile başlamalıdır.")
        String githubUrl,

        @Size(max = 255)
        @Pattern(regexp = "^$|^https://([a-z]{2,3}\\.)?linkedin\\.com/.+",
                message = "LinkedIn bağlantısı https://linkedin.com/ ile başlamalıdır.")
        String linkedinUrl
) {
}