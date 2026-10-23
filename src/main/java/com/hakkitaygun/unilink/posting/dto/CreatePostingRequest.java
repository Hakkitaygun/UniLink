package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.posting.enums.PostingStage;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record CreatePostingRequest(
        @NotNull
        PostingType type,

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 5000)
        String description,

        @Size(max = 150)
        String organizationName,

        PostingStage stage,

        @Size(max = 500)
        String benefitsNote,

        @NotNull
        @Future(message = "Son tarih gelecekte olmalıdır.")
        Instant deadline,

        @NotEmpty(message = "En az bir rol eklemelisiniz.")
        @Size(max = 10, message = "En fazla 10 rol ekleyebilirsiniz.")
        List<@Valid PostingRoleRequest> roles,

        @Size(max = 20, message = "En fazla 20 teknoloji seçebilirsiniz.")
        Set<
                @NotBlank
                @Size(min = 2, max = 50)
                @Pattern(regexp = "^[\\p{L}\\p{N}.][\\p{L}\\p{N} .+#/&-]*$",
                        message = "Teknoloji adı geçersiz karakterler içeriyor.")
                        String> technologies
) {

    public CreatePostingRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
        organizationName = blankToNull(organizationName);
        benefitsNote = blankToNull(benefitsNote);
        technologies = technologies == null
                ? Set.of()
                : technologies.stream()
                .map(name -> name == null ? null : name.trim().replaceAll("\\s+", " "))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}