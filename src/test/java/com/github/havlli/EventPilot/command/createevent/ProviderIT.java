package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProviderIT extends TestDatabaseContainer {

    @Autowired
    private CreateEventInteraction underTest;

    @Test
    void correctlyCreatesTwoDifferentInstancesFromApplicationContext() {
        // Act
        var instanceOne = underTest.createNewInstance();
        var instanceTwo = underTest.createNewInstance();

        // Assert
        assertThat(instanceOne).isNotEqualTo(instanceTwo);
    }
}
