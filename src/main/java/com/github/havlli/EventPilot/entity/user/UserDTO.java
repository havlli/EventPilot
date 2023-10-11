package com.github.havlli.EventPilot.entity.user;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDTO(
        Long id,
        String username,
        String email,
        String password,
        Set<UserRole.Role> roles
) {
    public static UserDTO fromUser(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream().map(UserRole::getRole).collect(Collectors.toSet())
        );
    }
}
