package com.github.havlli.EventPilot.api.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank
        String username,
        @NotBlank
        String password
) {
    public static AuthRequest of(String username, String password) {
        return new AuthRequest(username, password);
    }
}
