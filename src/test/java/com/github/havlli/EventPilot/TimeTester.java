package com.github.havlli.EventPilot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.*;
import java.time.*;
import java.util.function.BiFunction;


public class TimeTester {

    private final EntityManager entityManager;
    private final PostgreSQLContainer<?> postgresSQLContainer;

    public TimeTester(EntityManager entityManager, PostgreSQLContainer<?> postgreSQLContainer) {
        this.entityManager = entityManager;
        this.postgresSQLContainer = postgreSQLContainer;
    }

    private static final String CURRENT_TIMESTAMP = "SELECT CURRENT_TIMESTAMP";

    public Instant getCurrentTimestampUsingPersistence() {
        Query query = entityManager.createNativeQuery(CURRENT_TIMESTAMP);

        Object singleResult = query.getSingleResult();
        return (Instant) singleResult;
    }

    public Instant getCurrentTimestampUsingJdbc() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                postgresSQLContainer.getJdbcUrl(),
                postgresSQLContainer.getUsername(),
                postgresSQLContainer.getPassword());
             Statement statement = connection.createStatement()
        ) {
            statement.execute(CURRENT_TIMESTAMP);
            ResultSet resultSet = statement.getResultSet();
            resultSet.next();
            java.sql.Timestamp timestamp = resultSet.getTimestamp(1);
            return timestamp.toInstant();
        }
    }

    public Instant getInstantNowFromSystem() {
        return Instant.now();
    }

    LocalDateTime getUtcTimeNow() {
        ZoneId utcZone = ZoneId.of("UTC");
        return LocalDateTime.now(utcZone);
    }

    LocalDateTime getLocalTimeNow() {
        ZoneId localZone = ZoneId.systemDefault();
        return LocalDateTime.now(localZone);
    }

    public BiFunction<Instant, Instant, Long> calculateTimeDifference = (instant1, instant2) -> Duration.between(instant1, instant2).toMillis();
}
