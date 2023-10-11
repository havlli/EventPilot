package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private AutoCloseable autoCloseable;
    private UserService underTest;
    @Mock
    private UserDAO userDAOMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new UserService(userDAOMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void saveUser() {
        // Arrange
        User user = new User("test", "test", "test");

        // Act
        underTest.saveUser(user);

        // Assert
        verify(userDAOMock).saveUser(user);
    }

    @Test
    void deleteUser_DeletesUser_WhenUserExists() {
        // Arrange
        when(userDAOMock.userExistsById(any())).thenReturn(true);

        // Act
        underTest.deleteUserById(1L);

        // Assert
        verify(userDAOMock).deleteUserById(1L);
    }

    @Test
    void deleteUser_ThrowsException_WhenUserNotExists() {
        // Arrange
        when(userDAOMock.userExistsById(any())).thenReturn(false);

        // Act
        assertThatThrownBy(() -> underTest.deleteUserById(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        // Assert
        verify(userDAOMock, never()).deleteUserById(1L);
    }

    @Test
    void findAllUsers() {
        // Act
        List<User> actual = underTest.findAllUsers();

        // Assert
        verify(userDAOMock).findAllUsers();
    }

    @Test
    void findUserById_ReturnsUser_WhenUserExists() {
        // Arrange
        User expected = new User();
        when(userDAOMock.findUserById(any())).thenReturn(Optional.of(expected));

        // Act
        User actual = underTest.findUserById(1L);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findUserById_ThrowsException_WhenUserNotExists() {
        // Arrange
        when(userDAOMock.findUserById(any())).thenReturn(Optional.empty());

        // Assert
        assertThatThrownBy(() -> underTest.findUserById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}