package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.api.ApiErrorResponse;
import com.github.havlli.EventPilot.api.auth.UserSignupRequest;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import com.github.havlli.EventPilot.entity.user.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class GuildControllerIT extends TestDatabaseContainer {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GuildRepository guildRepository;
    @Autowired
    private UserRepository userRepository;
    private static final String BASE_URI = "/api/guilds";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getAllGuilds_ReturnsListOfGuilds_WhenAuthenticatedUserRole() {
        // Arrange
        String bearerToken = signupUser("username", "password", "email");
        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        // Act & Assert
        webTestClient.get()
                .uri(BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Guild.class)
                .hasSize(2)
                .contains(guild);
    }

    @Test
    void getAllGuilds_ReturnApiErrorResponse_WhenNotAuthenticated() {
        // Arrange
        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        // Act & Assert
        ApiErrorResponse actual = webTestClient.get()
                .uri(BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(actual.path()).isEqualTo(BASE_URI);
    }

    // Helper methods
    private String signupUser(String username, String password, String email) {
        UserSignupRequest signupRequest = new UserSignupRequest(username, password, email);
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
