package com.github.havlli.EventPilot.entity.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 3, max = 25)
        String username,
        @Email
        @Size(min = 4, max = 50)
        String email,
        UserRole role
) {
    public User updateUser(User user) {
        User.Builder builder = User.builder().fromUser(user);

        if (username != null) builder.withUsername(username);
        if (email != null) builder.withEmail(email);
        if (role != null) builder.withRoles(role);

        return builder.build();
    }
}
