package com.github.havlli.EventPilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigTest {

    private Config underTest;

    @BeforeEach
    void setUp() {
        underTest = new Config();
    }
    @Test
    void pathMatchingResourcePatternResolver() {
        // Act
        PathMatchingResourcePatternResolver actual = underTest.pathMatchingResourcePatternResolver();

        // Assert
        assertNotNull(actual);
    }
}