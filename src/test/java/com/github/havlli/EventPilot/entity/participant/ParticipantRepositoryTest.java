package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventRepository;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ParticipantRepositoryTest extends TestDatabaseContainer {

    @Autowired
    private ParticipantRepository underTest;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private GuildRepository guildRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        System.out.printf("Number of beans initialized { %s }%n", applicationContext.getBeanDefinitionCount());
    }

    @Test
    void findAllByEvent_WillReturnList_WhenParticipantsAssignedToEvent() {
        // Arrange
        Guild guild = addDummyGuild();
        Event event = addDummyEvent();
        Participant participant1 = addDummyParticipant();
        Participant participant2 = new Participant("2", "user2", 1, 2, event);
        participant2 = addParticipant(participant2, event, guild);

        int expectedSize = 2;

        // Act
        List<Participant> actual = underTest.findAllByEvent(event);

        // Assert
        assertThat(actual.size()).isEqualTo(expectedSize);
        assertThat(actual).containsOnly(participant1, participant2);
    }

    @Test
    void findAllByEvent_WillReturnEmptyList_WhenNoParticipantsAssignedToEvent() {
        // Arrange
        Event event = addDummyEvent();

        int expectedSize = 0;

        // Act
        List<Participant> actual = underTest.findAllByEvent(event);

        // Assert
        assertThat(actual.size()).isEqualTo(expectedSize);
        assertThat(actual).isEmpty();
    }

    @Test
    void findParticipantByUserId_WillReturnParticipant_WhenExists() {
        // Arrange
        Participant participant = addDummyParticipant();
        String userId = participant.getUserId();

        // Act
        Optional<Participant> actual = underTest.findParticipantByUserId(userId);

        // Assert
        assertThat(actual).isPresent()
                .hasValueSatisfying(p -> {
            assertThat(p.getUserId()).isEqualTo(participant.getUserId());
            assertThat(p.getUsername()).isEqualTo(participant.getUsername());
            assertThat(p.getPosition()).isEqualTo(participant.getPosition());
            assertThat(p.getRoleIndex()).isEqualTo(participant.getRoleIndex());
        });
    }

    @Test
    void findParticipantByUserId_WillReturnEmptyOptional_WhenParticipantNotExists() {
        // Arrange
        String userId = "123456";

        // Act
        Optional<Participant> actual = underTest.findParticipantByUserId(userId);

        // Assert
        assertThat(actual).isEmpty();
    }

    Guild addGuild(Guild guild) {
        guildRepository.save(guild);
        Optional<Guild> actual = guildRepository.findById(guild.getId());

        assertThat(actual)
                .withFailMessage("Unable to fetch Guild object from guildRepository")
                .isPresent()
                .hasValueSatisfying(g -> {
                    assertThat(g.getId()).isEqualTo(guild.getId());
                    assertThat(g.getName()).isEqualTo(guild.getName());
                    assertThat(g.getEvents()).isEqualTo(guild.getEvents());
                });

        return actual.get();
    }

    Event addEvent(Event event, Guild guild) {
        if (!guildRepository.existsById(guild.getId())) {
            addGuild(guild);
        }
        eventRepository.save(event);
        Optional<Event> actual = eventRepository.findById(event.getEventId());
        System.out.println(actual.get().getGuild().getEvents());
        assertThat(actual)
                .isPresent()
                .hasValueSatisfying(e -> {
                    assertThat(e.getEventId()).isEqualTo(event.getEventId());
                    assertThat(e.getName()).isEqualTo(event.getName());
                    assertThat(e.getDescription()).isEqualTo(event.getDescription());
                    assertThat(e.getAuthor()).isEqualTo(event.getAuthor());
                    assertThat(e.getDateTime()).isEqualTo(event.getDateTime());
                    assertThat(e.getDestinationChannelId()).isEqualTo(event.getDestinationChannelId());
                    assertThat(e.getMemberSize()).isEqualTo(event.getMemberSize());
                    assertThat(e.getInstances()).isEqualTo(event.getInstances());
                    assertThat(e.getParticipants()).isEqualTo(event.getParticipants());
                });

        return actual.get();
    }

    Participant addParticipant(Participant participant, Event event, Guild guild) {
        if (!eventRepository.existsById(event.getEventId())) {
            addEvent(event, guild);
        }
        underTest.save(participant);
        Optional<Participant> actual = underTest.findParticipantByUserId(participant.getUserId());

        assertThat(actual)
                .isPresent()
                .hasValueSatisfying(p -> {
                    assertThat(p.getUserId()).isEqualTo(participant.getUserId());
                    assertThat(p.getUsername()).isEqualTo(participant.getUsername());
                    assertThat(p.getPosition()).isEqualTo(participant.getPosition());
                    assertThat(p.getRoleIndex()).isEqualTo(participant.getRoleIndex());
                });

        return actual.get();
    }

    Guild addDummyGuild() {
        return addGuild(dummyGuild);
    }

    Event addDummyEvent() {
        return addEvent(dummyEvent, dummyGuild);
    }

    Participant addDummyParticipant() {
        return addParticipant(dummyParticipant, dummyEvent, dummyGuild);
    }

    private final Guild dummyGuild = new Guild("1","guild");
    private final Event dummyEvent = new Event(
            "10",
            "event",
            "description",
            "123456789",
            Instant.now(),
            "123456789",
            null,
            "15",
            new ArrayList<>(),
            dummyGuild
    );
    private final Participant dummyParticipant = new Participant(
            "100",
            "user",
            1,
            2,
            dummyEvent
    );
}