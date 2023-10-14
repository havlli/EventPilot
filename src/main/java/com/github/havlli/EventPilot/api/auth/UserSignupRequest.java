package com.github.havlli.EventPilot.api.auth;

public record UserSignupRequest(
    String username,
    String email,
    String password
) {
    public static UserSignupRequest of(String username, String email, String password) {
        return new UserSignupRequest(username, email, password);
    }
}
