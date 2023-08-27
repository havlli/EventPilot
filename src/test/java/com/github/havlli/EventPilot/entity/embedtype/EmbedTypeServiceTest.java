package com.github.havlli.EventPilot.entity.embedtype;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class EmbedTypeServiceTest {

    private AutoCloseable autoCloseable;
    private EmbedTypeService underTest;
    @Mock
    private EmbedTypeDAO embedTypeDAO;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedTypeService(embedTypeDAO);
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
    void getEmbedTypeById() {
        // Arrange
        Integer id = 1;

        // Act
        underTest.getEmbedTypeById(id);

        // Assert
        verify(embedTypeDAO, only()).getEmbedTypeById(id);
    }
}