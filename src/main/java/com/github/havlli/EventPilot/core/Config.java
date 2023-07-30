package com.github.havlli.EventPilot.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class Config {

    @Bean
    public PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver();
    }
}
