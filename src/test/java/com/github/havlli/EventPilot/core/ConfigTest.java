package com.github.havlli.EventPilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(actual).isNotNull();
    }

    @Test
    void messageSource() {
        // Act
        ResourceBundleMessageSource actual = underTest.messageSource();

        //
        assertThat(actual).isNotNull();
    }
}