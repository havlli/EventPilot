package com.github.havlli.EventPilot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDBConnection extends AbstractDatabaseContainer{

    @Test
    void canStartPostgresDB() {
        assertThat(postgresSQLContainer.isCreated()).isTrue();
    }

    @Test
    void postgresDBInstanceRunning() {
        assertThat(postgresSQLContainer.isRunning()).isTrue();
    }

    @Test
    void canPopulatePostgresDB() throws SQLException, IOException {
        populateDummyData();
        assertThat(postgresSQLContainer.isRunning()).isTrue();
    }
}
