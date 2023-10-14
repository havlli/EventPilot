package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.api.ApiErrorResponse;
import com.github.havlli.EventPilot.api.auth.AuthRequest;
import com.github.havlli.EventPilot.api.auth.AuthResponse;
import com.github.havlli.EventPilot.api.auth.UserSignupRequest;
import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserRepository;
import com.github.havlli.EventPilot.entity.user.UserRole;
import com.github.havlli.EventPilot.entity.user.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerIT extends TestDatabaseContainer {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleService userRoleService;
    private static final String BASE_URI = "/api/auth";
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void authenticate_AuthenticateUserAndReturnsAuthResponse_WhenValidUserCredentials() {
        // Arrange
        UserSignupRequest signupRequest = UserSignupRequest.of("username",  "email", "password");

        webTestClient.post()
                .uri(BASE_URI + "/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(signupRequest), UserSignupRequest.class)
                .exchange()
                .expectStatus()
                .isCreated();

        AuthRequest authRequest = AuthRequest.of("username", "password");

        // Act
        String endpoint = BASE_URI + "/authenticate";
        AuthResponse actual = webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(authRequest), AuthRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual.token()).isNotNull();
        assertThat(actual.user().username()).isEqualTo("username");
    }

    @Test
    void authenticate_ReturnApiErrorResponse_WhenInvalidUserCredentials() {
        // Arrange
        AuthRequest authRequest = AuthRequest.of("username", "password");
        String endpoint = BASE_URI + "/authenticate";

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(authRequest), AuthRequest.class)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.path()).isEqualTo(endpoint);
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void signup_RegisterUserAndReturnsAuthResponse_WhenValidUserCredentials() {
        // Arrange
        UserSignupRequest signupRequest = UserSignupRequest.of("username", "password", "email");

        String endpoint = BASE_URI + "/signup";
        String regexExpectedLocation = "/api/users/.";

        // Act
        FluxExchangeResult<AuthResponse> fluxExchangeResult = webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(signupRequest), UserSignupRequest.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(AuthResponse.class);

        // Assert
        String uriPath = fluxExchangeResult
                .getResponseHeaders()
                .getLocation()
                .getPath();
        String token = fluxExchangeResult.getResponseHeaders().getFirst("Authorization");
        Long createdId = Long.parseLong(uriPath.substring(uriPath.lastIndexOf("/") + 1));

        assertThat(uriPath).matches(regexExpectedLocation);
        assertThat(token).isNotNull();
        assertThat(userRepository.findById(createdId)).isPresent();
    }

    @Test
    void signup_ReturnsApiErrorResponse_WhenUsernameAlreadyExists() {
        // Arrange
        UserRole defaultUserRole = userRoleService.getDefaultUserRole();
        userRepository.save(new User(
                null,
                "username",
                "password",
                "email",
                Set.of(defaultUserRole)));

        UserSignupRequest signupRequest = UserSignupRequest.of("username", "password", "email2");

        String endpoint = BASE_URI + "/signup";

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(signupRequest), UserSignupRequest.class)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.path()).isEqualTo(endpoint);
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.message()).isEqualTo("Username already taken");
    }

    @Test
    void signup_ReturnsApiErrorResponse_WhenEmailAlreadyExists() {
        // Arrange
        UserRole defaultUserRole = userRoleService.getDefaultUserRole();
        userRepository.save(new User(
                null,
                "username",
                "password",
                "email",
                Set.of(defaultUserRole)));

        UserSignupRequest signupRequest = UserSignupRequest.of("username1", "password", "email");

        String endpoint = BASE_URI + "/signup";

        // Act
        ApiErrorResponse actual = webTestClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(signupRequest), UserSignupRequest.class)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.path()).isEqualTo(endpoint);
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(actual.message()).isEqualTo("Email already taken");
    }
}