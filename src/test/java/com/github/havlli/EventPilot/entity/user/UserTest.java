package com.github.havlli.EventPilot.entity.user;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

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

}