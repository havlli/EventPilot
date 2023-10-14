package com.github.havlli.EventPilot.api.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    @Test
    void testToString() {
        // Arrange
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "username",
                "email",
                "password",
                List.of()
        );

        String expected = "UserDetailsImpl{" +
                "id=1, " +
                "username='username', " +
                "email='email', " +
                "password='password', " +
                "authorities=[]" +
                "}";

        // Act
        String actual = userDetails.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}