package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    private final UserRoleDAO userRoleRepository;

    public UserRoleService(UserRoleDAO userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    public UserRole findByRole(UserRole.Role role) {
        return userRoleRepository.findByRole(role)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole {%s} not found".formatted(role)));
    }

    public UserRole getDefaultUserRole() {
        return findByRole(UserRole.Role.USER);
    }
}
