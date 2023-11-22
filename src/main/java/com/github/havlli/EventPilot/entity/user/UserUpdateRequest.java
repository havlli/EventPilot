package com.github.havlli.EventPilot.entity.user;

import java.util.Set;

public record UserUpdateRequest(
        String username,
        String email,
        Set<UserRole> roles
) {
    public User updateUser(User user) {
        User.Builder builder = User.builder().fromUser(user);

        if (username != null) builder.withUsername(username);
        if (email != null) builder.withEmail(email);
        if (roles != null) builder.withRoles(roles);

        return builder.build();
    }
}
