package com.github.havlli.EventPilot.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ConfigTest {

    @Autowired
    private PathMatchingResourcePatternResolver patternResolver;

    @Test
    void pathMatchingResourcePatternResolver() {
        // Assert
        assertNotNull(patternResolver);
    }
}