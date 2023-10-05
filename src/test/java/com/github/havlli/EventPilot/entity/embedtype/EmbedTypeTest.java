package com.github.havlli.EventPilot.entity.embedtype;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbedTypeTest {

    private EmbedType underTest;

    @BeforeEach
    void setUp() {
        underTest = new EmbedType(1L,"test","placeholder", List.of());
    }

    @Test
    void testToString() {
        // Arrange
        String expected = "EmbedType{id=1, name='test', structure='placeholder'}";

        // Act
        String actual = underTest.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}