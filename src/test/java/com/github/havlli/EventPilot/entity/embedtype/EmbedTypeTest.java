package com.github.havlli.EventPilot.entity.embedtype;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

class EmbedTypeTest {

    private EmbedType underTest;

    @BeforeEach
    void setUp() {
        underTest = new EmbedType(1,"test","placeholder", List.of());
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