package com.github.havlli.EventPilot.entity.user;

import java.util.List;
import java.util.Optional;

public interface UserDAO {
    void saveUser(User user);
    void deleteUserById(Long id);
    boolean userExistsById(Long id);
    List<User> findAllUsers();
    Optional<User> findUserById(Long id);
}
