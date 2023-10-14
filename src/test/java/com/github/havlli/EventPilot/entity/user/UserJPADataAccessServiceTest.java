package com.github.havlli.EventPilot.entity.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class UserJPADataAccessServiceTest {

    private AutoCloseable autoCloseable;
    private UserJPADataAccessService underTestSpy;
    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTestSpy = spy(new UserJPADataAccessService(userRepository));
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void saveUser() {
        // Arrange
        User user = mock(User.class);

        // Act
        underTestSpy.saveUser(user);

        // Assert
        verify(userRepository, only()).save(user);
    }

    @Test
    void deleteUserById() {
        // Arrange
        Long id = 1L;

        // Act
        underTestSpy.deleteUserById(id);

        // Assert
        verify(userRepository, only()).deleteById(id);
    }

    @Test
    void userExistsById() {
        // Arrange
        Long id = 1L;

        // Act
        underTestSpy.userExistsById(id);

        // Assert
        verify(userRepository, only()).existsById(id);
    }

    @Test
    void findAllUsers() {
        // Act
        underTestSpy.findAllUsers();

        // Assert
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findUserById() {
        // Arrange
        Long id = 1L;

        // Act
        underTestSpy.findUserById(id);

        // Assert
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    void findUserByUsername() {
        // Arrange
        String username = "username";

        // Act
        underTestSpy.findUserByUsername(username);

        // Assert
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void existsByUsername() {
        // Arrange
        String username = "username";

        // Act
        underTestSpy.existsByUsername(username);

        // Assert
        verify(userRepository, times(1)).existsByUsername(username);
    }

    @Test
    void existsByEmail() {
        // Arrange
        String email = "email";

        // Act
        underTestSpy.existsByEmail(email);

        // Assert
        verify(userRepository, times(1)).existsByEmail(email);
    }
}