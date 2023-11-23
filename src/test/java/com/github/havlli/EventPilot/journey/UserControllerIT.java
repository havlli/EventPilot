package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.api.ApiErrorResponse;
import com.github.havlli.EventPilot.api.auth.UserSignupRequest;
import com.github.havlli.EventPilot.entity.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

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
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;

    private static final String USER_CONTROLLER_BASE_URI = "/api/users";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void updateUser_UpdatesUser_WhenUserExists() {
        // Arrange
        String username = "username";
        String bearerToken = signupUser(username, "password", "email");

        User fetchedUser = userRepository.findAll()
                .stream()
                .filter(user -> user.getUsername().equals("username"))
                .findFirst()
                .orElseThrow();

        String newUsername = "newUsername";
        String newEmail = "newEmail";
        UserRole newRole = userRoleRepository.findByRole(UserRole.Role.ADMIN).orElseThrow();
        UserUpdateRequest updateRequest = new UserUpdateRequest(newUsername, newEmail, newRole);

        // Act
        UserDTO actual = webTestClient.post()
                .uri(USER_CONTROLLER_BASE_URI + "/" + fetchedUser.getId() + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(Mono.just(updateRequest), UserUpdateRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDTO.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.username()).isEqualTo(newUsername);
        assertThat(actual.email()).isEqualTo(newEmail);
    }

    @Test
    void updateUser_ReturnsConflictResponse_WhenUsernameAlreadyExists() {
        // Arrange
        String username = "username";
        String bearerToken = signupUser(username, "password", "email");

        User fetchedUser = userRepository.findAll()
                .stream()
                .filter(user -> user.getUsername().equals("username"))
                .findFirst()
                .orElseThrow();

        String existingUsername = "newUsername";
        String newEmail = "newEmail";
        UserRole newRole = userRoleRepository.findByRole(UserRole.Role.ADMIN).orElseThrow();
        UserUpdateRequest updateRequest = new UserUpdateRequest(existingUsername, newEmail, newRole);

        signupUser(existingUsername, "password", "anotherEmail");

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(USER_CONTROLLER_BASE_URI + "/" + fetchedUser.getId() + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(Mono.just(updateRequest), UserUpdateRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.message()).contains("Key (username)");
    }

    @Test
    void updateUser_ReturnsConflictResponse_WhenEmailAlreadyExists() {
        // Arrange
        String username = "username";
        String bearerToken = signupUser(username, "password", "email");

        User fetchedUser = userRepository.findAll()
                .stream()
                .filter(user -> user.getUsername().equals("username"))
                .findFirst()
                .orElseThrow();

        String newUsername = "newUsername";
        String existingEmail = "existingEmail";
        UserRole newRole = userRoleRepository.findByRole(UserRole.Role.ADMIN).orElseThrow();
        UserUpdateRequest updateRequest = new UserUpdateRequest(newUsername, existingEmail, newRole);

        signupUser("user2", "password", existingEmail);

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(USER_CONTROLLER_BASE_URI + "/" + fetchedUser.getId() + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(Mono.just(updateRequest), UserUpdateRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.message()).contains("Key (email)");
    }

    @Test
    void updateUser_ReturnsConflictResponse_WhenUsernameAndEmailAlreadyExists() {
        // Arrange
        String username = "username";
        String bearerToken = signupUser(username, "password", "email");

        User fetchedUser = userRepository.findAll()
                .stream()
                .filter(user -> user.getUsername().equals("username"))
                .findFirst()
                .orElseThrow();

        String existingUsername = "existingUsername";
        String existingEmail = "existingEmail";
        UserRole newRole = userRoleRepository.findByRole(UserRole.Role.ADMIN).orElseThrow();
        UserUpdateRequest updateRequest = new UserUpdateRequest(existingUsername, existingEmail, newRole);

        signupUser(existingUsername, "password", existingEmail);

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(USER_CONTROLLER_BASE_URI + "/" + fetchedUser.getId() + "/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(Mono.just(updateRequest), UserUpdateRequest.class)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.message()).contains("Key (username)");
    }

    @Test
    void updateUser_Returns404Response_WhenUserDoesNotExists() {
        // Arrange
        String username = "username";
        String bearerToken = signupUser(username, "password", "email");

        String existingUsername = "existingUsername";
        String existingEmail = "existingEmail";
        UserRole newRole = userRoleRepository.findByRole(UserRole.Role.ADMIN).orElseThrow();
        UserUpdateRequest updateRequest = new UserUpdateRequest(existingUsername, existingEmail, newRole);

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(USER_CONTROLLER_BASE_URI + "/101/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(Mono.just(updateRequest), UserUpdateRequest.class)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actual.message()).contains("Cannot update user with id");
    }

    @Test
    void getUserById_ReturnsUserDTO_WhenUserExists() {
        // Arrange
        String bearerToken = signupUser("username", "password", "email");

        UserDTO expected = new UserDTO(null, "username", "email", null);
        List<User> actualUsers = webTestClient.get()
                .uri(USER_CONTROLLER_BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .returnResult()
                .getResponseBody();
        Long userId = actualUsers.stream()
                .filter(user -> user.getUsername().equals("username"))
                .findFirst()
                .map(User::getId)
                .orElseThrow();

        // Act
        UserDTO actual = webTestClient.get()
                .uri(USER_CONTROLLER_BASE_URI + "/" + userId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDTO.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).usingRecursiveComparison()
                .comparingOnlyFields("username", "email")
                .isEqualTo(expected);
    }

    @Test
    void getUserById_Returns404Error_WhenUserDoesNotExist() {
        // Arrange
        String bearerToken = signupUser("username", "password", "email");

        long userId = 3L;

        // Act
        ApiErrorResponse actual = webTestClient.get()
                .uri(USER_CONTROLLER_BASE_URI + "/" + userId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAllUsers_ReturnsListOfUserDTOs_WhenAuthenticatedUserRole() {
        // Arrange
        String bearerToken = signupUser("username", "password", "email");

        // Act
        List<User> actualUsers = webTestClient.get()
                .uri(USER_CONTROLLER_BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actualUsers).hasSize(1);

    }

    @Test
    void getAllUsers_ReturnsErrorResponseUnauthorized_WhenNotAuthenticated() {
        // Act
        ApiErrorResponse actual = webTestClient.get()
                .uri(USER_CONTROLLER_BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        System.out.println(actual);
        assertThat(actual).isInstanceOf(ApiErrorResponse.class);
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(actual.path()).isEqualTo("/api/users");
    }

    // Helper methods
    private String signupUser(String username, String password, String email) {
        UserSignupRequest signupRequest = new UserSignupRequest(username, email, password);
        String jwtToken = webTestClient.post()
                .uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(signupRequest), UserSignupRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(Void.class)
                .getResponseHeaders().get(HttpHeaders.AUTHORIZATION)
                .get(0);
        return String.format("Bearer %s", jwtToken);
    }
}
