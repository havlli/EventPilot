package com.github.havlli.EventPilot.api.auth;

public record AuthRequest(
        String username,
        String password
) {
    public static AuthRequest of(String username, String password) {
        return new AuthRequest(username, password);
    }
}
