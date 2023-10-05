package com.github.havlli.EventPilot.entity.embedtype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EmbedTypeServiceTest {

    private AutoCloseable autoCloseable;
    private EmbedTypeService underTest;
    @Mock
    private EmbedTypeDAO embedTypeDAO;
    @Mock
    private EmbedTypeSerialization serializationMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedTypeService(embedTypeDAO, serializationMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void getAllEmbedTypes() {
        // Act
        underTest.getAllEmbedTypes();

        // Assert
        verify(embedTypeDAO, only()).getAllEmbedTypes();
    }

    @Test
    void saveEmbedType() {
        // Arrange
        EmbedType embedTypeMock = mock(EmbedType.class);

        // Act
        underTest.saveEmbedType(embedTypeMock);

        // Assert
        verify(embedTypeDAO, only()).saveEmbedType(embedTypeMock);
    }

    @Test
    void getEmbedTypeById_ReturnsEmbedType_WhenEmbedTypePresent() {
        // Arrange
        Long id = 1L;
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(embedTypeDAO.getEmbedTypeById(id)).thenReturn(Optional.of(embedTypeMock));

        // Act
        underTest.getEmbedTypeById(id);

        // Assert
        verify(embedTypeDAO, only()).getEmbedTypeById(id);
    }

    @Test
    void getEmbedTypeById_ThrowsException_WhenEmbedTypeNotPresent() {
        // Arrange
        Long id = 1L;

        // Assert
        assertThatThrownBy(() -> underTest.getEmbedTypeById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("EmbedType with id {%s} was not found!".formatted(id));
        verify(embedTypeDAO, only()).getEmbedTypeById(id);
    }

    @Test
    void existsEmbedEventById() {
        // Arrange
        Long id = 1L;

        // Act
        underTest.existsEmbedEventById(id);

        // Assert
        verify(embedTypeDAO, only()).existsEmbedTypeById(id);
    }

    @Test
    void getDeserializedMap() throws JsonProcessingException {
        // Arrange
        String structure = "json_structure";
        EmbedType embedType = mock(EmbedType.class);
        when(embedType.getStructure()).thenReturn(structure);

        // Act
        underTest.getDeserializedMap(embedType);

        // Assert
        verify(serializationMock, only()).deserializeMap(structure);
    }

    @Test
    void validateJsonOrThrow_ValidatesAndDoesntThrow_WhenJsonBlobIsValid() throws JsonProcessingException {
        // Arrange
        String validJson = """
                {"-1":"Absence","-2":"Late","1":"Tank","-3":"Tentative","2":"Melee","3":"Ranged","4":"Healer","5":"Support"}
                """;

        // Act
        underTest.validateJsonOrThrow(validJson, new RuntimeException());

        // Assert
        verify(serializationMock, only()).deserializeMap(validJson);
    }

    @Test
    void validateJsonOrThrow_ValidatesAndThrows_WhenJsonBlobIsInvalid() throws JsonProcessingException {
        // Arrange
        String json = "";
        when(serializationMock.deserializeMap(json)).thenThrow(mock(JsonProcessingException.class));

        // Act
        assertThatThrownBy(() -> underTest.validateJsonOrThrow(json, new RuntimeException()))
                .isInstanceOf(RuntimeException.class);

        //
        verify(serializationMock, only()).deserializeMap(json);
    }
}