package com.github.havlli.EventPilot.entity.embedtype;

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
        Integer id = 1;
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
        Integer id = 1;

        // Assert
        assertThatThrownBy(() -> underTest.getEmbedTypeById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("EmbedType with id {%s} was not found!".formatted(id));
        verify(embedTypeDAO, only()).getEmbedTypeById(id);
    }

    @Test
    void existsEmbedEventById() {
        // Arrange
        Integer id = 1;

        // Act
        underTest.existsEmbedEventById(id);

        // Assert
        verify(embedTypeDAO, only()).existsEmbedTypeById(id);
    }
}