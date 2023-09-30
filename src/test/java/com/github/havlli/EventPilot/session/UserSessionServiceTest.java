package com.github.havlli.EventPilot.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserSessionServiceTest {

    private AutoCloseable autoCloseable;
    private UserSessionService underTest;
    @Mock
    private SessionStorage sessionStorageMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new UserSessionService(sessionStorageMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void clearDatabaseOnInitialization() {
        // Act
        underTest.clearDatabaseOnInitialization();

        // Assert
        verify(sessionStorageMock, only()).clear();
    }

    @Test
    void createUserSession_CreatesAndReturnsUserSession_WhenUserSessionDoesNotExist() {
        // Arrange
        String userId = "1234";
        String username = "test";
        when(sessionStorageMock.exists(userId)).thenReturn(false);

        Optional<UserSession> expected = Optional.of(new UserSession(userId, username));

        // Act
        Optional<UserSession> actual = underTest.createUserSession(userId, username);

        // Assert
        verify(sessionStorageMock, times(1)).save(userId, username);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createUserSession_ReturnsEmptyUserSession_WhenUserSessionAlreadyExist() {
        // Arrange
        String userId = "1234";
        String username = "test";
        when(sessionStorageMock.exists(userId)).thenReturn(true);

        Optional<UserSession> expected = Optional.empty();

        // Act
        Optional<UserSession> actual = underTest.createUserSession(userId, username);

        // Assert
        verify(sessionStorageMock, never()).save(userId, username);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void terminateUserSession() {
        // Arrange
        String userId = "1234";

        // Act
        underTest.terminateUserSession(userId);

        // Assert
        verify(sessionStorageMock, only()).remove(userId);
    }
}