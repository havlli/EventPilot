package com.github.havlli.EventPilot.entity.user;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

class UserRoleTest {

    @Test
    void testConstructor() {
        // Act
        UserRole userRole = new UserRole(UserRole.Role.USER);
        UserRole userRoleNoArg = new UserRole();

        // Assert
        assertThat(userRole).isNotNull();
        assertThat(userRoleNoArg).isNotNull();
    }

    @Test
    void testHashCode() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        int expectedHash = Objects.hash(userRole.getId(), userRole.getRole());

        // Act
        int actualHash = userRole.hashCode();

        // Assert
        assertThat(actualHash).isEqualTo(expectedHash);
    }

    @Test
    void testToString() {
        // Arrange
        UserRole.Role role = UserRole.Role.USER;

        UserRole userRole = new UserRole(role);
        String expectedString = "UserRole{" +
                "id=" + null +
                ", role=" + role +
                '}';
        // Act
        String actualString = userRole.toString();

        // Assert
        assertThat(actualString).isEqualTo(expectedString);
    }

    @Test
    void testEquals() {
        // Arrange
        UserRole userRole1 = new UserRole(UserRole.Role.USER);
        UserRole userRole2 = new UserRole(UserRole.Role.USER);

        // Act
        boolean actualEquals = userRole1.equals(userRole2);

        // Assert
        assertThat(actualEquals).isTrue();
    }

}