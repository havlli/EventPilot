package com.github.havlli.EventPilot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the application against an empty database and relies on startup to apply the migrations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
class FlywayStartupMigrationIT {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("eventpilot_empty")
            .withUsername("user")
            .withPassword("password");


    static {
        Startables.deepStart(Stream.of(POSTGRES)).join();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("discord.token", () -> "test");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startupAppliesAllMigrationsToEmptyDatabase() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        assertThat(jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema() AND table_name IN ('embed_type', 'event', 'guild', 'participant')
                ORDER BY table_name
                """, String.class))
                .containsExactly("embed_type", "event", "guild", "participant");
    }
}
