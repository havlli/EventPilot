package com.github.havlli.EventPilot.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmbedFormatterTest {

    private EmbedFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new EmbedFormatter();
    }

    @Test
    void raidSize() {
        // Arrange
        int current = 5;
        int maximum = 10;
        String pattern = "%s/%s";
        String expected = String.format(pattern, current, maximum);

        // Act
        String actual = underTest.raidSize(current, maximum);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void leaderWithId() {
        // Arrange
        String leader = "leader";
        String id = "10";
        String pattern = "Leader: %s - ID: %s";
        String expected = String.format(pattern, leader, id);

        // Act
        String actual = underTest.leaderWithId(leader, id);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void date() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:D>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.date(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void time() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:t>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.time(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void dateTime() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:f>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.dateTime(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void relativeTime() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:R>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.relativeTime(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}