package com.github.havlli.EventPilot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Testcontainers
public abstract class AbstractDatabaseContainer {

    public static final String SCHEMA_SQL = "src/test/resources/schema.sql";
    public static final String CLEANUP_SQL = "src/test/resources/cleanup.sql";
    public static final String DATA_SQL = "src/test/resources/data.sql";

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        setupSchema();
    }

    @BeforeEach
    void beforeEach() throws SQLException, IOException {
        clearAllData();
    }

    @Container
    protected static PostgreSQLContainer<?> postgresSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("eventpilot")
            .withUsername("user")
            .withPassword("password");

    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresSQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgresSQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresSQLContainer.getPassword());
    }

    protected static void executeSQLFile(String sqlResourcePath) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword());
             Statement statement = connection.createStatement()
        ) {
            String schemaSql = new String(Files.readAllBytes(Paths.get(sqlResourcePath)));
            statement.execute(schemaSql);
            System.out.println(statement.getUpdateCount());
        }
    }

    protected void execute(String sqlQuery) throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword());
             Statement statement = connection.createStatement()
        ) {
            statement.execute(sqlQuery);
        }
    }

    private static void setupSchema() throws SQLException, IOException {
        executeSQLFile(SCHEMA_SQL);
    }

    protected void clearAllData() throws SQLException, IOException {
        executeSQLFile(CLEANUP_SQL);
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
}
