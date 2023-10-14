package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.api.auth.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

public record UserDTO(
        Long id,
        String username,
        String email,
        Set<String> roles
) {
    public static UserDTO of(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(UserRole::getRole)
                        .map(UserRole.Role::name)
                        .collect(Collectors.toSet())
        );
    }

    public static UserDTO fromUserDetails(UserDetailsImpl userDetails) {
        return new UserDTO(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
        );
    }
}
