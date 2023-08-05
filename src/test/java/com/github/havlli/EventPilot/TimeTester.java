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
        Instant instant = (Instant) singleResult;
        Timestamp timestamp = Timestamp.from(instant);
        LocalDateTime localDateTimeUTCZone = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        LocalDateTime localDateTimeLocalZone = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        System.out.printf("""
                        =========== CURRENT_TIMESTAMP from JPA ==============
                        instant = { %s }
                        timestamp = { %s }
                        localDateTimeUTCZone = { %s } { %s }
                        localDateTimeLocalZone = { %s } { %s }
                        =====================================================
                        """,
                instant,
                timestamp,
                localDateTimeUTCZone,
                ZoneId.of("UTC"),
                localDateTimeLocalZone,
                ZoneId.systemDefault()
        );
        return instant;
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
            Instant instant = timestamp.toInstant();
            LocalDateTime localDateTimeUTCZone = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
            LocalDateTime localDateTimeLocalZone = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            System.out.printf("""
                            =========== CURRENT_TIMESTAMP from JDBC ============
                            instant = { %s }
                            timestamp = { %s }
                            localDateTimeUTCZone = { %s } { %s }
                            localDateTimeLocalZone = { %s } { %s }
                            ====================================================
                            """,
                    instant,
                    timestamp,
                    localDateTimeUTCZone,
                    ZoneId.of("UTC"),
                    localDateTimeLocalZone,
                    ZoneId.systemDefault()
            );

            return instant;
        }
    }

    public Instant getInstantNowFromSystem() {
        Instant instant = Instant.now();
        System.out.printf("""
                =========  System Instant Now  ================
                instant = { %s }
                ===============================================
                """, instant
        );

        return instant;
    }

    LocalDateTime getUtcTimeNow() {
        ZoneId utcZone = ZoneId.of("UTC");
        LocalDateTime utcTime = LocalDateTime.now(utcZone);
        Instant instant = utcTime.toInstant(ZoneOffset.UTC);
        printTimeInfo(utcTime, utcZone, instant);

        return utcTime;
    }

    LocalDateTime getLocalTimeNow() {
        ZoneId localZone = ZoneId.systemDefault();
        LocalDateTime localTime = LocalDateTime.now(localZone);
        Instant instant = localTime.toInstant(ZoneOffset.of(localZone.getId()));
        printTimeInfo(localTime, localZone, instant);

        return localTime;
    }

    void printTimeInfo(LocalDateTime localDateTime, ZoneId zoneId, Instant instant) {
        System.out.printf("""
                        =====================================================
                        Fetching LocalDateTime Now
                        zoneId = { %s }
                        localDateTime = { %s }
                        Instant.from(localTime) = { %s }
                        =====================================================
                        """,
                zoneId.getId(),
                localDateTime,
                instant
        );
    }

    public BiFunction<Instant, Instant, Long> calculateTimeDifference = (instant1, instant2) -> Duration.between(instant1, instant2).toMillis();
}