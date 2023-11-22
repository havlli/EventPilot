package com.github.havlli.EventPilot.entity.user;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void testAllArgConstructor() {
        // Act
        User user = new User(1L,"test", "test", "test", new HashSet<>());

        // Assert
        assertThat(user).isNotNull();
    }

    @Test
    void testHashCode() {
        // Arrange
        User user = new User(1L,"test", "test", "test", new HashSet<>());
        int expectedHash = Objects.hash(1L, "test", "test", "test", new HashSet<>());

        // Act
        int actualHash = user.hashCode();

        // Assert
        assertThat(actualHash).isEqualTo(expectedHash);
    }

    @Test
    void testToString() {
        // Arrange
        long userId = 1L;
        String username = "test";
        String email = "test";
        String password = "test";
        HashSet<UserRole> roles = new HashSet<>();
        User user = new User(userId, username, email, password, roles);
        String expectedString = "User{" +
                "id=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", roles=" + roles +
                '}';

        // Act
        String actualString = user.toString();

        // Assert
        assertThat(actualString).isEqualTo(expectedString);
    }

    @Test
    void testBuilder() {
        // Act
        long id = 1L;
        String username = "test";
        String email = "test";
        String password = "test";
        User.Builder builder = User.builder()
                .withId(id)
                .withUsername(username)
                .withEmail(email)
                .withPassword(password)
                .withRoles(new UserRole(UserRole.Role.USER), new UserRole(UserRole.Role.ADMIN));

        HashSet<UserRole> roles = new HashSet<>();
        builder.withRoles(roles);

        // Act & Assert
        assertThatThrownBy(() -> builder.getUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot retrieve user that was not built yet!");

        User actual = builder.build();

        assertThat(actual).isNotNull();
        assertThat(builder.getUser()).isNotNull();
        assertThat(actual.getUsername()).isEqualTo(username);
        assertThat(actual.getEmail()).isEqualTo(email);
        assertThat(actual.getPassword()).isEqualTo(password);
        assertThat(actual.getRoles()).isEqualTo(roles);
    }
}