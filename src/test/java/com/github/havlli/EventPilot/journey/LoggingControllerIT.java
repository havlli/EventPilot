package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.api.jwt.JWTService;
import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserRepository;
import com.github.havlli.EventPilot.entity.user.UserRole;
import com.github.havlli.EventPilot.entity.user.UserRoleRepository;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoggingControllerIT extends TestDatabaseContainer {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void consoleLogStream_ReturnsUnauthorized_WhenAnonymous() {
        webTestClient.get().uri("/api/logging/stream-sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void consoleLogStream_EmitsStreamOfConsoleEvents_WhenAdminAuthenticated() {
        // Act
        var responseBody = webTestClient.get().uri("/api/logging/stream-sse")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(UserRole.Role.ADMIN))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .returnResult(ServerSentEvent.class)
                .getResponseBody();

        // Assert
        StepVerifier
                .create(responseBody)
                .expectSubscription()
                .expectNextCount(5)
                .thenCancel()
                .verify();
    }

    @Test
    void consoleLogStream_ReturnsForbidden_WhenUserAuthenticated() {
        webTestClient.get().uri("/api/logging/stream-sse")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerTokenFor(UserRole.Role.USER))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    private String bearerTokenFor(UserRole.Role role) {
        UserRole userRole = userRoleRepository.findByRole(role).orElseThrow();
        User user = userRepository.save(new User(
                null,
                "logging-%s".formatted(role.name().toLowerCase()),
                "%s@example.test".formatted(role.name().toLowerCase()),
                "password",
                Set.of(userRole)
        ));
        return "Bearer %s".formatted(jwtService.generateToken(user));
    }
}
