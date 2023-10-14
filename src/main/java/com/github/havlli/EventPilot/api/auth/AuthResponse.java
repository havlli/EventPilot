package com.github.havlli.EventPilot.api.auth;

import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserDTO;

public record AuthResponse(
        String token,
        UserDTO user
) {
    public static AuthResponse of(String token, UserDetailsImpl user) {
        return new AuthResponse(token, UserDTO.fromUserDetails(user));
    }

    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, UserDTO.of(user));
    }
}
