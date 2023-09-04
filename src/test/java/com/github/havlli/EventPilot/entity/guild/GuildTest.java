package com.github.havlli.EventPilot.entity.guild;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

class GuildTest {

    private Guild underTest;

    @BeforeEach
    void setUp() {
        underTest = new Guild("1","guild", List.of());
    }

    @Test
    void Guild_toString() {
        // Arrange
        String expected = "Guild{snowflakeId='1', guildName='guild'}";

        // Act
        String actual = underTest.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}