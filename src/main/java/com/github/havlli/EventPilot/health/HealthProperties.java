package com.github.havlli.EventPilot.health;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eventpilot.health")
public record HealthProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("8081") @Min(1) @Max(65535) int port
) {
}
