package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserControllerIT extends TestDatabaseContainer {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserService userService;

    @Test
    void getAllUsers_ReturnsListOfUserDTOs() {
        // Arrange
        User user = new User("test", "test", "test");
        userService.saveUser(user);

        // Act
        List<User> actualUsers = webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actualUsers).containsOnly(user);
    }
}
