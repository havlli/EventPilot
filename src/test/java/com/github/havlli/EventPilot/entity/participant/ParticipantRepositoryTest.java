package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeRepository;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventRepository;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ParticipantRepositoryTest extends TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(ParticipantRepositoryTest.class);
    @Autowired
    private ParticipantRepository underTest;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private GuildRepository guildRepository;
    @Autowired
    private EmbedTypeRepository embedTypeRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    public void beforeEach() throws SQLException, IOException {
        LOG.info("Clearing database....");
        clearAllData();
        LOG.info("Number of beans initialized {}", applicationContext.getBeanDefinitionCount());
    }

    @Test
    public void saveParticipant_SavesParticipantToDatabase_WhenParticipantNotExists() throws SQLException {
        // Arrange
        Guild validGuild = new Guild("1", "guild1");
        addGuildWithNativeQuery(validGuild);
        EmbedType embedType = addDummyEmbedType();

        Event validEvent = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                validGuild,
                embedType);
        eventRepository.save(validEvent);

        Participant participant = new Participant("1", "user1", 1, 1, validEvent);
        Optional<Participant> expected = Optional.of(participant);

        // Act
        underTest.save(participant);

        // Assert
        Optional<Participant> actual = underTest.findParticipantByUserId(participant.getUserId());
        assertThat(actual).isPresent()
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void saveParticipant_UpdatesParticipant_WhenParticipantAlreadyExists() throws SQLException {
        // Arrange
        Guild validGuild = new Guild("1", "guild1");
        addGuildWithNativeQuery(validGuild);
        EmbedType embedType = addDummyEmbedType();

        Event validEvent = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                validGuild,
                embedType);
        eventRepository.save(validEvent);

        Participant participant = new Participant("1", "user1", 1, 1, validEvent);
        underTest.save(participant);
        Participant participantInDatabase = underTest.findParticipantByUserId(participant.getUserId()).get();

        Participant updatedParticipant = new Participant(participantInDatabase.getId(), "2", "updated-user1", 1, 1, validEvent);
        Optional<Participant> expected = Optional.of(updatedParticipant);

        // Act
        underTest.save(updatedParticipant);

        // Assert
        List<Participant> actualList = underTest.findAll();
        assertThat(actualList).hasSize(1);
        Optional<Participant> actual = underTest.findParticipantByUserId(participant.getUserId());
        assertThat(actual).isPresent()
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void findAllByEvent_WillReturnList_WhenParticipantsAssignedToEvent() {
        // Arrange
        Guild guild = addDummyGuild();
        Event event = addDummyEvent();
        EmbedType embedType = addDummyEmbedType();
        Participant participant1 = addDummyParticipant();
        Participant participant2 = new Participant("2", "user2", 1, 2, event);
        participant2 = addParticipant(participant2, event, guild, embedType);

        int expectedSize = 2;

        // Act
        List<Participant> actual = underTest.findAllByEvent(event);

        // Assert
        assertThat(actual.size()).isEqualTo(expectedSize);
        assertThat(actual).containsOnly(participant1, participant2);
    }

    @Test
    public void findAllByEvent_WillReturnEmptyList_WhenNoParticipantsAssignedToEvent() {
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
    public void findParticipantByUserId_WillReturnParticipant_WhenExists() {
        // Arrange
        Participant participant = addDummyParticipant();
        String userId = participant.getUserId();

        // Act
        Optional<Participant> actual = underTest.findParticipantByUserId(userId);

        // Assert
        assertThat(actual).isPresent()
                .hasValueSatisfying(p -> assertThat(p)
                        .usingRecursiveComparison()
                        .isEqualTo(participant));
    }

    @Test
    public void findParticipantByUserId_WillReturnEmptyOptional_WhenParticipantNotExists() {
        // Arrange
        String userId = "123456";

        // Act
        Optional<Participant> actual = underTest.findParticipantByUserId(userId);

        // Assert
        assertThat(actual).isEmpty();
    }

    public Guild addGuild(Guild guild) {
        guildRepository.save(guild);
        Optional<Guild> actual = guildRepository.findById(guild.getId());

        assertThat(actual)
                .withFailMessage("Unable to fetch Guild object from guildRepository")
                .isPresent()
                .hasValueSatisfying(g -> assertThat(g).isEqualTo(guild));

        return actual.get();
    }

    public Event addEvent(Event event, Guild guild, EmbedType embedType) {
        if (!guildRepository.existsById(guild.getId())) {
            addGuild(guild);
        }
        if (!embedTypeRepository.existsById(embedType.getId())) {
            addEmbedType(embedType);
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

    public Participant addParticipant(Participant participant, Event event, Guild guild, EmbedType embedType) {
        if (!eventRepository.existsById(event.getEventId())) {
            addEvent(event, guild, embedType);
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

    public EmbedType addEmbedType(EmbedType embedType) {
        embedTypeRepository.save(embedType);
        Optional<EmbedType> actual = embedTypeRepository.findById(embedType.getId());

        assertThat(actual).hasValueSatisfying(e -> {
            assertThat(e.getId()).isEqualTo(embedType.getId());
            assertThat(e.getName()).isEqualTo(embedType.getName());
            assertThat(e.getStructure()).isEqualTo(embedType.getStructure());
            assertThat(e.getEvents()).usingRecursiveComparison().isEqualTo(embedType.getEvents());
        });

        return actual.get();
    }

    public Guild addDummyGuild() {
        return addGuild(dummyGuild);
    }

    public EmbedType addDummyEmbedType() {
        return addEmbedType(dummyEmbedType);
    }

    public Event addDummyEvent() {
        return addEvent(dummyEvent, dummyGuild, dummyEmbedType);
    }

    public Participant addDummyParticipant() {
        return addParticipant(dummyParticipant, dummyEvent, dummyGuild, dummyEmbedType);
    }

    private final Guild dummyGuild = new Guild("1", "guild");

    private final EmbedType dummyEmbedType = new EmbedType(
            1,
            "test",
            "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
            new ArrayList<>()
    );
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
            dummyGuild,
            dummyEmbedType
    );
    private final Participant dummyParticipant = new Participant(
            "100",
            "user",
            1,
            2,
            dummyEvent
    );
}