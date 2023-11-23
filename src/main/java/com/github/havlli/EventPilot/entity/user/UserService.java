package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.exception.DuplicateResourceException;
import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserDAO userRepository;
    private final UserRoleService userRoleService;

    public UserService(UserDAO userRepository, UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
    }

    public void saveUser(User user) {
        userRepository.saveUser(user);
    }

    public void deleteUserById(Long id) {
        if (!userRepository.userExistsById(id)) {
            throw new ResourceNotFoundException("Cannot delete user with id {%s} - does not exist!".formatted(id));
        }
        userRepository.deleteUserById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findAllUsers();
    }

    public User findUserById(Long id) {
        return userRepository.findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id {%s} was not found!".formatted(id)));
    }

    public void checkUsernameAvailability(String username) {
        if(userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already taken");
        }
    }

    public void checkEmailAvailability(String email){
        if(userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already taken");
        }
    }

    public User updateUser(Long id, UserUpdateRequest updateRequest) {

        Optional<User> userOptional = userRepository.findUserById(id);

        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("Cannot update user with id {%s} - does not exist!".formatted(id));
        }

        UserRole userRole = userRoleService.findByRole(updateRequest.role().getRole());

        User updatedUser = User.builder()
                .fromUser(updateRequest.updateUser(userOptional.orElseThrow()))
                .withRoles(userRole)
                .build();

        userRepository.saveUser(updatedUser);

        return updatedUser;
    }
}
