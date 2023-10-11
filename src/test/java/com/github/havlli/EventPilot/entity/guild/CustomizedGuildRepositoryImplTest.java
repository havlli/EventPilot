package com.github.havlli.EventPilot.entity.guild;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class CustomizedGuildRepositoryImplTest {

    private AutoCloseable autoCloseable;
    private CustomizedGuildRepositoryImpl underTest;
    @Mock
    private EntityManager entityManagerMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = spy(new CustomizedGuildRepositoryImpl(entityManagerMock));
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void saveGuildIfNotExists() {
        // Arrange
        String id = "1";
        String name = "test";

        Query query = mock(Query.class);
        when(entityManagerMock.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(1, id)).thenReturn(query);
        when(query.setParameter(2, name)).thenReturn(query);
        when(query.setParameter(3, id)).thenReturn(query);

        // Act
        underTest.saveGuildIfNotExists(id, name);

        // Assert
        verify(query).setParameter(1, id);
        verify(query).setParameter(2, name);
        verify(query).setParameter(3, id);
        verify(query).executeUpdate();
    }
}