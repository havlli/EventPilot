package com.github.havlli.EventPilot;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractDatabaseContainer {

    @BeforeAll
    static void beforeAll() {

    }

    @Container
    protected static PostgreSQLContainer<?> postgresSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("event-pilot-unit-test")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresSQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgresSQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresSQLContainer.getPassword());
    }
}
