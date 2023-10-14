package com.github.havlli.EventPilot.entity.user;

import java.util.Optional;

public interface UserRoleDAO {
    Optional<UserRole> findByRole(UserRole.Role role);
}
