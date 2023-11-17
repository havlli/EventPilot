package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.api.auth.UserSignupRequest;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeRepository;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventRepository;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantRepository;
import com.github.havlli.EventPilot.entity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ParticipantControllerIT extends TestDatabaseContainer {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private GuildRepository guildRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EmbedTypeRepository embedTypeRepository;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private UserRepository userRepository;

    private static final String BASE_URI = "/api/participants";

    @BeforeEach
    void setUp() {
        deleteAll();
    }

    @Test
    void deleteParticipant_DeletesParticipant_WhenUserAuthenticatedAndParticipantExists() {
        // Arrange
        Guild guild = createGuild();
        EmbedType embedType = createEmbedType();
        Event event = createEvent(guild, embedType);

        Participant participantOne = createParticipant("123","userOne", event);
        Participant participantTwo = createParticipant("456","userTwo", event);

        String authorizationToken = signupUser("userOne", "password", "ama@ama.am");

        // Act
        System.out.println(participantOne.getId());
        webTestClient.delete()
                .uri(BASE_URI + "/" + participantOne.getId())
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                .exchange()
                .expectStatus().isOk();

        // Assert
        assertThat(participantRepository.findAll()).hasSize(1);
    }

    @Test
    void deleteParticipant_ReturnsNotFound_WhenUserAuthenticatedAndParticipantNotExists() {
        // Arrange
        Guild guild = createGuild();
        EmbedType embedType = createEmbedType();
        Event event = createEvent(guild, embedType);

        Participant participantOne = createParticipant("123","userOne", event);
        Participant participantTwo = createParticipant("456","userTwo", event);

        String authorizationToken = signupUser("userOne", "password", "ama@ama.am");

        // Act
        webTestClient.delete()
                .uri(BASE_URI + "/2323232323")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                .exchange()
                .expectStatus().isNotFound();

        // Assert
        assertThat(participantRepository.findAll()).hasSize(2);
    }

    @Test
    void deleteParticipant_ReturnsUnauthorized_WhenUserNotAuthenticated() {
        // Arrange
        Guild guild = createGuild();
        EmbedType embedType = createEmbedType();
        Event event = createEvent(guild, embedType);

        Participant participantOne = createParticipant("123","userOne", event);
        Participant participantTwo = createParticipant("456","userTwo", event);

        String authorizationToken = signupUser("userOne", "password", "ama@ama.am");

        // Act
        webTestClient.delete()
                .uri(BASE_URI + "/2323232323")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();

        // Assert
        assertThat(participantRepository.findAll()).hasSize(2);
    }

    // Helper Methods
    private Guild createGuild() {
        Guild guild = new Guild("1", "guild");
        return guildRepository.save(guild);
    }

    private EmbedType createEmbedType() {
        EmbedType embedType = EmbedType.builder()
                .withName("test")
                .withStructure("test")
                .build();
        return embedTypeRepository.save(embedType);
    }

    private Event createEvent(Guild guild, EmbedType embedType) {
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
        guild.getEvents().add(event);
        embedType.getEvents().add(event);
        return eventRepository.save(event);
    }

    private Participant createParticipant(String userId, String username, Event event) {
        Participant participant = new Participant(userId, username, 1, 1, event);
        event.getParticipants().add(participant);
        return participantRepository.save(participant);
    }

    private void printAll() {
        System.out.println("Printing all...");
        System.out.println("Guilds =====================");
        guildRepository.findAll().forEach(System.out::println);
        System.out.println("Embed Types =====================");
        embedTypeRepository.findAll().forEach(System.out::println);
        System.out.println("Events =================");
        eventRepository.findAll().forEach(System.out::println);
        System.out.println("Participants ===============");
        participantRepository.findAll().forEach(System.out::println);
    }

    private void deleteAll() {
        guildRepository.deleteAll();
        embedTypeRepository.deleteAll();
        eventRepository.deleteAll();
        participantRepository.deleteAll();
        userRepository.deleteAll();
    }

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