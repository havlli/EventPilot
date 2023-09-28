package com.github.havlli.EventPilot.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

class SessionStorageTest {

    private AutoCloseable autoCloseable;
    private SessionStorage underTest;
    @Mock
    private SessionDAO sessionDAO;
    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new SessionStorage(sessionDAO);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }
    @Test
    void save() {
        // Arrange
        String key = "key";
        String value = "value";

        // Act
        underTest.save(key, value);

        // Assert
        verify(sessionDAO, only()).save(key, value);
    }

    @Test
    void exists() {
        // Act
        underTest.exists("key");

        // Assert
        verify(sessionDAO, only()).exists("key");
    }

    @Test
    void remove() {
        // Act
        underTest.remove("key");

        // Assert
        verify(sessionDAO, only()).remove("key");
    }

    @Test
    void clear() {
        // Act
        underTest.clear();

        // Assert
        verify(sessionDAO, only()).clear();
    }
}