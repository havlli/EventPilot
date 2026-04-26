package com.github.havlli.EventPilot;

import com.github.havlli.EventPilot.entity.guild.Guild;
import com.redis.testcontainers.RedisContainer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
public abstract class TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(TestDatabaseContainer.class);
    public static final String SCHEMA_SQL = "src/test/resources/schema.sql";
    public static final String CLEANUP_SQL = "src/test/resources/cleanup.sql";
    public static final String DATA_SQL = "src/test/resources/data.sql";

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        Flyway flyway = Flyway.configure().dataSource(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword()
        ).load();
        flyway.migrate();
        logContainerInfo();
    }

    protected static PostgreSQLContainer<?> postgresSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("eventpilot")
            .withUsername("user")
            .withPassword("password");

    protected static RedisContainer redisContainer = new RedisContainer("redis:7.2.1-alpine");

    static {
        Startables.deepStart(Stream.of(postgresSQLContainer, redisContainer)).join();
    }

    @DynamicPropertySource
    private static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresSQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgresSQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresSQLContainer.getPassword());
        registry.add("spring.data.redis.host", () -> redisContainer.getHost());
        registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort());
        registry.add("security.jwt.secret", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        registry.add("discord.token", () -> "test");
    }

    protected static void executeSQLFile(String sqlResourcePath) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword());
             Statement statement = connection.createStatement()
        ) {
            String sql = new String(Files.readAllBytes(Paths.get(sqlResourcePath)));
            statement.execute(sql);
            System.out.println(statement.getUpdateCount());
        }
    }

    protected ResultSet execute(String sqlQuery) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword());
             Statement statement = connection.createStatement()
        ) {
            statement.execute(sqlQuery);
            return statement.getResultSet();
        }
    }

    protected <T> T executeMapObject(String sqlQuery, Function<ResultSet, T> mapper) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword());
             Statement statement = connection.createStatement()
        ) {
            statement.execute(sqlQuery);
            return mapper.apply(statement.getResultSet());
        }
    }

    public Function<ResultSet, Guild> mapResultToGuild = result -> {
        try {
            result.next();
            return new Guild(
                    result.getString("id"),
                    result.getString("name")
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    };

    protected static void setupSchema() throws SQLException, IOException {
        executeSQLFile(SCHEMA_SQL);
    }

    protected void clearAllData() throws SQLException, IOException {
        executeSQLFile(CLEANUP_SQL);
    }

    @BeforeEach
    void resetDatabaseState() throws SQLException, IOException {
        clearAllData();
    }


    protected static void populateDummyData() throws SQLException, IOException {
        executeSQLFile(DATA_SQL);
    }

    protected String getExecPsqlCommand() {
        return String.format("docker exec -it %s psql -U %s -d %s",
                postgresSQLContainer.getContainerId(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getDatabaseName());
    }

    protected static void logContainerInfo() {
        LOG.info("postgresSQLContainer.isCreated() - {}",postgresSQLContainer.isCreated());
        LOG.info("postgresSQLContainer.isRunning() - {}",postgresSQLContainer.isRunning());
        LOG.info("postgresSQLContainer.getJdbcUrl() - {}",postgresSQLContainer.getJdbcUrl());
        LOG.info("postgresSQLContainer.isHostAccessible() - {}",postgresSQLContainer.isHostAccessible());
    }

    public Guild addGuildWithNativeQuery(Guild guild) throws SQLException {
        String insertGuildQuery = String.format("""
                        INSERT INTO guild (id, name) VALUES ('%s', '%s');
                        """,
                guild.getId(),
                guild.getName()
        );

        execute(insertGuildQuery);

        String selectGuildQuery = String.format("""
                        SELECT * FROM guild WHERE id = '%s';
                        """,
                guild.getId()
        );
        Guild actual = executeMapObject(selectGuildQuery, mapResultToGuild);

        assertThat(actual).isEqualTo(guild);

        return guild;
    }
}
