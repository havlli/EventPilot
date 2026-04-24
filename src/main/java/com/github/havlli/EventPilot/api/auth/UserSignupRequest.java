package com.github.havlli.EventPilot.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
    @NotBlank
    @Size(min = 3, max = 25)
    String username,
    @NotBlank
    @Email
    @Size(min = 4, max = 50)
    String email,
    @NotBlank
    @Size(min = 8, max = 120)
    String password
) {
    public static UserSignupRequest of(String username, String email, String password) {
        return new UserSignupRequest(username, email, password);
    }
}
