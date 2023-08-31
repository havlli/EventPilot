package com.github.havlli.EventPilot.entity.embedtype;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class EmbedTypeJPADataAccessServiceTest {

    private AutoCloseable autoCloseable;
    private EmbedTypeJPADataAccessService underTest;
    @Mock
    private EmbedTypeRepository embedTypeRepositoryMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedTypeJPADataAccessService(embedTypeRepositoryMock);
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
        verify(embedTypeRepositoryMock, only()).findAll();
    }

    @Test
    void getEmbedTypeById() {
        // Arrange
        Integer id = 1;

        // Act
        underTest.getEmbedTypeById(id);

        // Assert
        verify(embedTypeRepositoryMock, only()).findById(id);
    }

    @Test
    void saveEmbedType() {
        // Arrange
        EmbedType embedTypeMock = mock(EmbedType.class);

        // Act
        underTest.saveEmbedType(embedTypeMock);

        // Assert
        verify(embedTypeRepositoryMock, only()).save(embedTypeMock);
    }

    @Test
    void existsEmbedTypeById() {
        // Arrange
        Integer id = 1;

        // Act
        underTest.existsEmbedTypeById(id);

        // Assert
        verify(embedTypeRepositoryMock, only()).existsById(id);
    }
}