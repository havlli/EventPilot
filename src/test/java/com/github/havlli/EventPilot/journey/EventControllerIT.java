package com.github.havlli.EventPilot.journey;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeRepository;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventDTO;
import com.github.havlli.EventPilot.entity.event.EventRepository;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

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

    @Test
    void getAllEvents_ReturnsListOfEventDTOs() {
        // Arrange
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        System.out.println(guildRepository.findAll());
        System.out.println(guildRepository.existsById("1075050744719364156"));
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

        List<EventDTO> expected = Stream.of(event).map(EventDTO::fromEvent).toList();

        // Act
        List<EventDTO> actual = webTestClient.get()
                .uri("/api/events")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EventDTO.class)
                .returnResult().getResponseBody();

        // Assert
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("dateTime").isEqualTo(expected);
    }

    @Test
    void deleteEventById_deletesEvent_WhenEventExists() {
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        System.out.println(guildRepository.findAll());
        System.out.println(guildRepository.existsById("1075050744719364156"));
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

        // Act
        webTestClient.delete()
                .uri("/api/events/" + eventId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        // Assert
        assertThat(eventRepository.existsById(eventId)).isFalse();
    }

    @Test
    void deleteEventById_Returns404_WhenEventNotExists() {
        EmbedType embedType = EmbedType.builder().withName("test").withStructure("test").build();
        System.out.println(guildRepository.findAll());
        System.out.println(guildRepository.existsById("1075050744719364156"));
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

        // Act
        webTestClient.delete()
                .uri("/api/events/111111")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();

        // Assert
        assertThat(eventRepository.findAll()).hasSize(1);
    }
}
