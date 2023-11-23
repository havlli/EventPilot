package com.github.havlli.EventPilot.entity.user;

public record UserUpdateRequest(
        String username,
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
