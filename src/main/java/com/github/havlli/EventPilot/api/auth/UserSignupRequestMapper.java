package com.github.havlli.EventPilot.api.auth;

import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserSignupRequestMapper {

    private final PasswordEncoder passwordEncoder;

    public UserSignupRequestMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User mapUser(UserSignupRequest signupRequest, Set<UserRole> userRoles) {
        return new User(
                null,
                signupRequest.username(),
                signupRequest.email(),
                passwordEncoder.encode(signupRequest.password()),
                userRoles
        );
    }
}
