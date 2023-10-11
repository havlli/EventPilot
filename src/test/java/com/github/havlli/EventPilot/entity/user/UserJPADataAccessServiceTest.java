package com.github.havlli.EventPilot.entity.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        List<User> actual = underTestSpy.findAllUsers();

        // Assert
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findUserById() {
        // Arrange
        Long id = 1L;

        // Act
        Optional<User> actual = underTestSpy.findUserById(id);

        // Assert
        verify(userRepository, times(1)).findById(id);
    }
}