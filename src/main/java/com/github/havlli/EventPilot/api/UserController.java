package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.entity.user.UserDTO;
import com.github.havlli.EventPilot.entity.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> userDTOList = userService.findAllUsers()
                .stream()
                .map(UserDTO::of)
                .toList();

        return ResponseEntity.ok()
                .body(userDTOList);
    }
}
