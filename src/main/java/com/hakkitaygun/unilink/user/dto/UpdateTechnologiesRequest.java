package com.hakkitaygun.unilink.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record UpdateTechnologiesRequest(
        @NotNull
        @Size(max = 20, message = "En fazla 20 teknoloji seçebilirsiniz.")
        Set<
                @NotBlank
                @Size(min = 2, max = 50)
                @Pattern(regexp = "^[\\p{L}\\p{N}.][\\p{L}\\p{N} .+#/&-]*$",
                        message = "Teknoloji adı geçersiz karakterler içeriyor.")
                        String> technologies
) {

        public UpdateTechnologiesRequest {
                if (technologies != null) {
                        technologies = technologies.stream()
                                .map(name -> name == null ? null : name.trim().replaceAll("\\s+", " "))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                }
        }
}