package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.exception.InvalidDateTimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeServiceTest {

    private TimeService underTest;

    @BeforeEach
    void setUp() {
        underTest = new TimeService();
    }

    @Test
    void parseUtcInstant_ReturnsCorrectInstant() {
        // Arrange
        String dateTime = "21.08.2023 17:45";
        String formatPattern = "dd.MM.yyyy HH:mm";

        Instant expected = Instant.parse("2023-08-21T17:45:00.000Z");

        // Act
        Instant actual = underTest.parseUtcInstant(dateTime, formatPattern);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void parseUtcInstant_ThrowsDateTimeParseException_WhenFormatMismatch() {
        // Arrange
        String dateTime = "21.o8.2o23 I7:45";
        String formatPattern = "dd.MM.yyyy HH:mm";

        // Assert
        assertThatThrownBy(() -> underTest.parseUtcInstant(dateTime, formatPattern))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void isValidFutureTime_ReturnsTrue_WhenInstantIsInFuture() {
        // Arrange
        Instant givenInstant = Instant.now().plus(5, ChronoUnit.MILLIS);

        // Act
        boolean actual = underTest.isValidFutureTime(givenInstant);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void isValidFutureTime_ThrowsInvalidDateTimeException_WhenInstantIsNotInFuture() {
        // Arrange
        Instant givenInstant = Instant.now().minus(5, ChronoUnit.MILLIS);

        // Assert
        assertThatThrownBy(() -> underTest.isValidFutureTime(givenInstant))
                .isInstanceOf(InvalidDateTimeException.class);
    }
}