package com.github.havlli.EventPilot.entity.embedtype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmbedTypeSerializationTest {

    private AutoCloseable autoCloseable;
    private EmbedTypeSerialization underTest;
    @Mock
    private ObjectMapper objectMapperMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedTypeSerialization(objectMapperMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void serializeDefaultMap() throws JsonProcessingException {
        // Act
        underTest.serializeDefaultMap();

        // Assert
        verify(objectMapperMock, only()).writeValueAsString(anyMap());
    }

    @Test
    void serializeMap() throws JsonProcessingException {
        // Arrange
        HashMap<Integer, String> mapMock = mock(HashMap.class);

        // Act
        underTest.serializeMap(mapMock);

        // Assert
        verify(objectMapperMock, only()).writeValueAsString(mapMock);
    }

    @Test
    void deserializeMap() throws JsonProcessingException {
        // Arrange
        String structure = "json_structure";

        // Act
        underTest.deserializeMap(structure);

        // Assert
        verify(objectMapperMock, only()).readValue(eq(structure), any(TypeReference.class));
    }
}