package com.github.havlli.EventPilot;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDatabaseConnectionIT extends TestDatabaseContainer {

    @Test
    void canStartPostgresDB() {
        assertThat(postgresSQLContainer.isCreated()).isTrue();
    }

    @Test
    void postgresDBInstanceRunning() {
        assertThat(postgresSQLContainer.isRunning()).isTrue();
    }

    @Test
    void canQueryPostgresDB() throws SQLException {
        Integer actual = executeMapObject("SELECT 1", resultSet -> {
            try {
                resultSet.next();
                return resultSet.getInt(1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(actual).isEqualTo(1);
    }
}
