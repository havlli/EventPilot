package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserRepositoryTest extends TestDatabaseContainer {

    public static final Logger LOG = LoggerFactory.getLogger(UserRepositoryTest.class);

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private UserRepository underTest;

    @BeforeEach
    void setUp() {
        LOG.info("Number of beans initialized { %s }".formatted(applicationContext.getBeanDefinitionCount()));
    }

    @Test
    void saveUser_SavesUser_WhenUserNotExists() {
        // Arrange
        User expected = new User("user", "email", "password");

        // Act
        underTest.save(expected);

        // Assert
        List<User> actualUsers = underTest.findAll();
        assertThat(actualUsers).containsOnly(expected);
    }

    @Test
    void saveUser_UpdatesUser_WhenUserAlreadyExists() {
        // Arrange
        User old = new User("user", "email", "password");
        underTest.save(old);
        Long oldUserId = underTest.findAll().stream()
                .filter(user -> user.getUsername().equals(old.getUsername()))
                .map(User::getId)
                .findFirst()
                .get();

        User updated = new User(oldUserId,"user", "email", "newpassword");

        // Act
        underTest.save(updated);

        // Assert
        List<User> actualUsers = underTest.findAll();
        assertThat(actualUsers).containsOnly(updated);
    }

    @Test
    void findAll_ReturnsAllUsers_WhenUsersExist() {
        // Arrange
        User user1 = new User("user1", "email", "password");
        User user2 = new User("user2", "email2", "password");
        underTest.save(user1);
        underTest.save(user2);

        // Act
        List<User> actualUsers = underTest.findAll();

        // Assert
        assertThat(actualUsers).containsOnly(user1, user2);
    }

    @Test
    void findAll_ReturnsNoUsers_WhenNoUsersExist() {
        // Act
        List<User> actualUsers = underTest.findAll();

        // Assert
        assertThat(actualUsers).hasSize(0);
    }

    @Test
    void deleteUserById_DeletesUser_WhenUserExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);
        Long userId = underTest.findAll().stream()
                .filter(u -> u.getUsername().equals(user.getUsername()))
                .map(User::getId)
                .findFirst()
                .get();

        // Act
        underTest.deleteById(userId);

        // Assert
        List<User> actualUsers = underTest.findAll();
        assertThat(actualUsers).isEmpty();
        assertThat(actualUsers).doesNotContain(user);
    }

    @Test
    void deleteUserById_DoNotDeletesUser_WhenUserNotExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);

        // Act
        underTest.deleteById(1234567L);

        // Assert
        List<User> actualUsers = underTest.findAll();
        assertThat(actualUsers).containsOnly(user);
    }

    @Test
    void userExistsById_ReturnsTrue_WhenUserExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);
        Long userId = underTest.findAll().stream()
                .filter(u -> u.getUsername().equals(user.getUsername()))
                .map(User::getId)
                .findFirst()
                .get();

        // Act
        boolean actual = underTest.existsById(userId);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void userExistsById_ReturnsFalse_WhenUserNotExists() {
        // Arrange

        // Act
        boolean actual = underTest.existsById(1L);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void findUserById_ReturnsOptionalUser_WhenUserExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);
        Long userId = underTest.findAll().stream()
                .filter(u -> u.getUsername().equals(user.getUsername()))
                .map(User::getId)
                .findFirst()
                .get();

        // Act
        Optional<User> actual = underTest.findById(userId);

        // Assert
        assertThat(actual).hasValue(user);
    }

    @Test
    void findUserById_ReturnsEmptyOptional_WhenUserNotExists() {
        // Act
        Optional<User> actual = underTest.findById(1L);

        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    void findByUsername_ReturnsOptionalUser_WhenUsernameExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);
        String username = user.getUsername();

        // Act
        Optional<User> actual = underTest.findByUsername(username);

        // Assert
        assertThat(actual).hasValue(user);
    }

    @Test
    void findByUsername_ReturnsEmptyOptional_WhenUsernameNotExists() {
        // Arrange
        String username = "not-existing";

        // Act
        Optional<User> actual = underTest.findByUsername(username);

        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    void existsByUsername_ReturnsTrue_WhenUsernameExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);
        String username = user.getUsername();

        // Act
        boolean actual = underTest.existsByUsername(username);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void existsByUsername_ReturnsFalse_WhenUsernameNotExists() {
        // Arrange
        String username = "not-existing";

        // Act
        boolean actual = underTest.existsByUsername(username);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void existsByEmail_ReturnsTrue_WhenEmailExists() {
        // Arrange
        User user = new User("user", "email", "password");
        underTest.save(user);
        String email = user.getEmail();

        // Act
        boolean actual = underTest.existsByEmail(email);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void existsByEmail_ReturnsFalse_WhenEmailNotExists() {
        // Arrange
        String email = "not-existing";

        // Act
        boolean actual = underTest.existsByEmail(email);

        // Assert
        assertThat(actual).isFalse();
    }
}
