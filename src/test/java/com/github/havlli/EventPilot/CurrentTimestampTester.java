package com.github.havlli.EventPilot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


public class CurrentTimestampTester {

    private final EntityManager entityManager;

    public CurrentTimestampTester(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Instant getCurrentTimestamp() {
        Query query = entityManager.createNativeQuery("SELECT CURRENT_TIMESTAMP");

        Object singleResult = query.getSingleResult();
        Instant instant = (Instant) singleResult;
        Timestamp timestamp = Timestamp.from(instant);
        LocalDateTime localDateTimeUTCZone = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        LocalDateTime localDateTimeLocalZone = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        System.out.printf("""
                        =====================================================
                        Fetching CURRENT_TIMESTAMP from JPA
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
}