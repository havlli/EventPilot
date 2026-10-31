package com.github.havlli.EventPilot.core;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void missingOrBlankToken_ReportsClearMessage(String token) {
        DiscordProperties properties = new DiscordProperties(
                token,
                new DiscordProperties.Commands("commands"),
                new DiscordProperties.Scheduler(60, 60)
        );

        Set<ConstraintViolation<DiscordProperties>> violations = validator.validate(properties);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(DiscordProperties.MISSING_TOKEN_MESSAGE);
    }
}
