package com.github.havlli.EventPilot.entity.user;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRoleJPADataAccessService implements UserRoleDAO {

    private final UserRoleRepository userRoleRepository;

    public UserRoleJPADataAccessService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public Optional<UserRole> findByRole(UserRole.Role role) {
        return userRoleRepository.findByRole(role);
    }
}
