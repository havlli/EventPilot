package com.github.havlli.EventPilot.entity.user;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void saveUser(User user) {
        userDAO.saveUser(user);
    }

    public void deleteUserById(Long id) throws ResourceNotFoundException {
        if (!userDAO.userExistsById(id)) {
            throw new ResourceNotFoundException("Cannot delete user with id {%s} - does not exist!".formatted(id));
        }
        userDAO.deleteUserById(id);
    }

    public List<User> findAllUsers() {
        return userDAO.findAllUsers();
    }

    public User findUserById(Long id) throws ResourceNotFoundException {
        return userDAO.findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id {%s} was not found!".formatted(id)));
    }
}
