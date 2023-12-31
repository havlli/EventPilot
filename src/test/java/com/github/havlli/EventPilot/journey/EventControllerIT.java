package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.api.ApiErrorResponse;
import com.github.havlli.EventPilot.api.auth.UserSignupRequest;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeRepository;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventDTO;
import com.github.havlli.EventPilot.entity.event.EventRepository;
import com.github.havlli.EventPilot.entity.event.EventUpdateRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EventControllerIT extends TestDatabaseContainer {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private GuildRepository guildRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EmbedTypeRepository embedTypeRepository;
    @Autowired
    private UserRepository userRepository;
    private static final String BASE_URI = "/api/events";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getEventById_ReturnsEvent_WhenEventExists() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        String eventId = "1234567890";
        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId(eventId)
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        guild.getEvents().add(event);

        String bearerToken = signupUser("username", "password", "email");

        // Act
        EventDTO actual = webTestClient.get()
                .uri(BASE_URI + "/" + eventId)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EventDTO.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.eventId()).isEqualTo(event.getEventId());
        assertThat(actual.name()).isEqualTo(event.getName());
        assertThat(actual.description()).isEqualTo(event.getDescription());
        assertThat(actual.author()).isEqualTo(event.getAuthor());
    }

    @Test
    void getEventById_ReturnsApiErrorResponse_WhenEventDoesNotExists() {
        // Arrange
        String bearerToken = signupUser("username", "password", "email");

        // Act
        ApiErrorResponse actual = webTestClient.get()
                .uri(BASE_URI + "/12")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAllEvents_ReturnsListOfEventDTOs_WhenUserAuthenticated() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId("1234567890")
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        guild.getEvents().add(event);
        List<EventDTO> expected = Stream.of(event).map(EventDTO::fromEvent).toList();

        String bearerToken = signupUser("username", "password", "email");

        // Act
        List<EventDTO> actual = webTestClient.get()
                .uri(BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventDTO.class)
                .returnResult().getResponseBody();

        // Assert
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("dateTime").isEqualTo(expected);
    }

    @Test
    void getAllEvents_ReturnsApiErrorResponse_WhenNotAuthenticated() {
        // Act
        ApiErrorResponse actual = webTestClient.get()
                .uri(BASE_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(actual.path()).isEqualTo(BASE_URI);
    }

    @Test
    void deleteEventById_DeletesEventAndReturnsNoContentStatus_WhenAuthenticatedAndEventExists() {
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        String eventId = "1234567890";
        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId(eventId)
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        String bearerToken = signupUser("username", "password", "email");
        String endpoint = BASE_URI + "/" + eventId;

        // Act
        webTestClient.delete()
                .uri(endpoint)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isNoContent();

        // Assert
        assertThat(eventRepository.existsById(eventId)).isFalse();
    }

    @Test
    void deleteEventById_Returns404_WhenAuthenticatedAndEventDoesNotExist() {
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        String eventId = "1234567890";
        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId(eventId)
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        String bearerToken = signupUser("username", "password", "email");
        String nonExistentEventId = "111111";
        String endpoint = BASE_URI + "/" + nonExistentEventId;

        // Act
        ApiErrorResponse actual = webTestClient.delete()
                .uri(endpoint)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();


        // Assert
        assertThat(eventRepository.findAll()).hasSize(1);
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(actual.path()).isEqualTo(endpoint);
    }

    @Test
    void deleteEventById_ReturnsApiErrorResponse_WhenNotAuthenticated() {
        // Arrange
        String endpoint = BASE_URI + "/123";

        // Act
        ApiErrorResponse actual = webTestClient.delete()
                .uri(endpoint)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(actual.path()).isEqualTo(endpoint);
    }

    @Test
    void getLastFiveEvents_ReturnsListOfEventDTOs_WhenUserAuthenticated() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        Event event1 = Event.builder()
                .withEmbedType(embedType)
                .withEventId("1234567890")
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event1);

        Event event2 = Event.builder()
                .withEmbedType(embedType)
                .withEventId("12345678901")
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event2);

        guild.getEvents().add(event1);
        guild.getEvents().add(event2);
        List<EventDTO> expected = Stream.of(event1, event2)
                .map(EventDTO::fromEvent).toList();

        String bearerToken = signupUser("username", "password", "email");

        // Act
        List<EventDTO> actual = webTestClient.get()
                .uri(BASE_URI + "/last")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventDTO.class)
                .returnResult().getResponseBody();

        // Assert
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("dateTime")
                .containsAll(expected);
    }

    @Test
    void updateEvent_UpdatesAndReturnsEventDTO_WhenIdExistsAndUserAuthenticated() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId("1234567890")
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        String authorizationToken = signupUser("username", "password", "email");

        String expectedName = "updatedName";
        String expectedDesc = "updatedDescription";
        String expectedSize = "5";
        EventUpdateRequest updateRequest = new EventUpdateRequest(expectedName, expectedDesc, expectedSize);

        // Act
        EventDTO actual = webTestClient.post()
                .uri(BASE_URI + "/" + event.getEventId())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                .body(Mono.just(updateRequest), EventUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(EventDTO.class)
                .returnResult()
                .getResponseBody();

        System.out.println(actual);

        // Assert
        assertThat(actual).hasFieldOrPropertyWithValue("name", expectedName);
        assertThat(actual).hasFieldOrPropertyWithValue("description", expectedDesc);
        assertThat(actual).hasFieldOrPropertyWithValue("memberSize", expectedSize);
    }

    @Test
    void updateEvent_UpdatesAndReturnsEventDTO_WhenIdExistsAndUserAuthenticatedAndPartialEventDetails() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId("1234567890")
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        String authorizationToken = signupUser("username", "password", "email");

        String expectedName = null;
        String expectedDesc = "updatedDescription";
        String expectedSize = null;
        EventUpdateRequest updateRequest = new EventUpdateRequest(expectedName, expectedDesc, expectedSize);

        // Act
        EventDTO actual = webTestClient.post()
                .uri(BASE_URI + "/" + event.getEventId())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                .body(Mono.just(updateRequest), EventUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(EventDTO.class)
                .returnResult()
                .getResponseBody();

        // Assert
        assertThat(actual).hasFieldOrPropertyWithValue("name", event.getName());
        assertThat(actual).hasFieldOrPropertyWithValue("description", expectedDesc);
        assertThat(actual).hasFieldOrPropertyWithValue("memberSize", event.getMemberSize());
    }

    @Test
    void updateEvent_RespondsWithNotFound_WhenEventNotExists() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        embedTypeRepository.save(embedType);

        Guild guild = new Guild("1234", "guild");
        guildRepository.save(guild);

        Event event = Event.builder()
                .withEmbedType(embedType)
                .withEventId("1234567890")
                .withName("Preview name")
                .withDescription("Preview description")
                .withAuthor("Author")
                .withDateTime(Instant.now())
                .withGuild(guild)
                .withDestinationChannel("1234567890")
                .withMemberSize("25")
                .build();
        eventRepository.save(event);

        String authorizationToken = signupUser("username", "password", "email");

        String expectedName = "updatedName";
        String expectedDesc = "updatedDescription";
        String expectedSize = "5";
        EventUpdateRequest updateRequest = new EventUpdateRequest(expectedName, expectedDesc, expectedSize);

        // Act
        webTestClient.post()
                .uri(BASE_URI + "/66666666")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                .body(Mono.just(updateRequest), EventUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void updateEvent_UpdatesNothing_WhenUserNotAuthenticated() {
        // Arrange
        String expectedName = "updatedName";
        String expectedDesc = "updatedDescription";
        String expectedSize = "5";
        EventUpdateRequest updateRequest = new EventUpdateRequest(expectedName, expectedDesc, expectedSize);

        // Act
        webTestClient.post()
                .uri(BASE_URI + "/1")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateRequest), EventUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isUnauthorized();
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
