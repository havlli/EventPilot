package com.github.havlli.EventPilot.core;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        @NotBlank String token,
        @Valid @NotNull Commands commands,
        @Valid @NotNull Scheduler scheduler
) {

    public record Commands(@NotBlank String folder) {
    }

    public record Scheduler(@Min(1) int intervalSeconds) {
    }
}
