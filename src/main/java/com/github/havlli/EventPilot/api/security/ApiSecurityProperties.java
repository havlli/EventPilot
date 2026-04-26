package com.github.havlli.EventPilot.api.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "security")
public record ApiSecurityProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors
) {

    public record Jwt(@NotBlank String secret) {
    }

    public record Cors(
            @NotEmpty List<String> allowedOrigins,
            @NotEmpty List<String> allowedMethods,
            @NotEmpty List<String> allowedHeaders,
            List<String> exposedHeaders
    ) {
    }
}
