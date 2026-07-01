package com.github.havlli.EventPilot.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventStatus;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantStatus;
import com.github.havlli.EventPilot.generator.EmbedFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OrganizerEventFormatterTest {

    private AutoCloseable autoCloseable;
    @Mock
    private EmbedTypeService embedTypeService;
    private OrganizerEventFormatter underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new OrganizerEventFormatter(new EmbedFormatter(), embedTypeService);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void formatEventList_IncludesRosterCountsStatusChannelAndMessageId() throws JsonProcessingException {
        // Arrange
        Event event = createEvent();
        event.getParticipants().add(new Participant("1", "tank", 1, 1, event));
        Participant waitlisted = new Participant("2", "backup", 2, 2, event);
        waitlisted.setStatus(ParticipantStatus.WAITLISTED);
        event.getParticipants().add(waitlisted);

        // Act
        String actual = underTest.formatEventList(List.of(event));

        // Assert
        assertThat(actual)
                .contains("Events:")
                .contains("**Raid Night**")
                .contains("Open")
                .contains("<#channel-1>")
                .contains("1/2 confirmed, 1 waitlisted")
                .contains("ID: `event-1`");
    }

    @Test
    void formatEventDetails_IncludesRoleGroupsAndWaitlist() throws JsonProcessingException {
        // Arrange
        Event event = createEvent();
        HashMap<Integer, String> roleNames = new HashMap<>();
        roleNames.put(1, "Tank");
        roleNames.put(2, "Healer");
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(roleNames);

        event.getParticipants().add(new Participant("1", "tank", 1, 1, event));
        Participant waitlisted = new Participant("2", "backup", 2, 2, event);
        waitlisted.setStatus(ParticipantStatus.WAITLISTED);
        event.getParticipants().add(waitlisted);

        // Act
        String actual = underTest.formatEventDetails(event);

        // Assert
        assertThat(actual)
                .contains("Event: **Raid Night**")
                .contains("ID: `event-1`")
                .contains("Roster: 1/2 confirmed, 1 waitlisted")
                .contains("Tank (1): `1` tank")
                .contains("`2` backup - Healer");
    }

    @Test
    void formatEventDetails_UsesEmptyStateAndUnknownChannel_WhenEventHasNoParticipantsOrChannel() throws JsonProcessingException {
        // Arrange
        Event event = createEventWithChannel(null);
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(new HashMap<>());

        // Act
        String actual = underTest.formatEventDetails(event);

        // Assert
        assertThat(actual)
                .contains("Channel: unknown channel")
                .contains("Roster: 0/2 confirmed, 0 waitlisted")
                .contains("No confirmed participants.")
                .contains("No waitlisted participants.");
    }

    @Test
    void formatEventDetails_UsesFallbackRoleNames_WhenRoleNamesCannotBeParsed() throws JsonProcessingException {
        // Arrange
        Event event = createEvent();
        when(embedTypeService.getDeserializedMap(event.getEmbedType()))
                .thenThrow(new JsonProcessingException("invalid role json") { });

        event.getParticipants().add(new Participant("1", "healer", 1, 2, event));
        event.getParticipants().add(new Participant("2", "tank", 2, 1, event));
        event.getParticipants().add(new Participant("3", "late", 3, -2, event));
        event.getParticipants().add(new Participant("4", "absence", 4, -1, event));
        Participant waitlisted = new Participant("5", "backup", 5, 99, event);
        waitlisted.setStatus(ParticipantStatus.WAITLISTED);
        event.getParticipants().add(waitlisted);

        // Act
        String actual = underTest.formatEventDetails(event);

        // Assert
        assertThat(actual)
                .contains("Role 1 (1): `2` tank")
                .contains("Role 2 (1): `1` healer")
                .contains("Role -2 (1): `3` late")
                .contains("Role -1 (1): `4` absence")
                .contains("`5` backup - Role 99");
    }

    @Test
    void formatEventList_TruncatesContent_WhenDiscordMessageWouldBeTooLong() {
        // Arrange
        List<Event> events = IntStream.range(0, 80)
                .mapToObj(index -> createEventWithName("Raid Night " + index))
                .toList();

        // Act
        String actual = underTest.formatEventList(events);

        // Assert
        assertThat(actual)
                .hasSize(1900)
                .endsWith("\n... truncated");
    }

    @Test
    void formatEventList_DoesNotCountNonCapacityOrNullRoleParticipantsAsConfirmed() {
        // Arrange
        Event event = createEvent();
        event.getParticipants().add(new Participant("1", "tank", 1, 1, event));
        event.getParticipants().add(new Participant("2", "late", 2, -1, event));
        event.getParticipants().add(new Participant("3", "missing-role", 3, null, event));

        // Act
        String actual = underTest.formatEventList(List.of(event));

        // Assert
        assertThat(actual).contains("1/2 confirmed, 0 waitlisted");
    }

    private Event createEvent() {
        return createEventWithChannel("channel-1");
    }

    private Event createEventWithName(String name) {
        return createEvent("event-" + name.hashCode(), name, "channel-1");
    }

    private Event createEventWithChannel(String channelId) {
        return createEvent("event-1", "Raid Night", channelId);
    }

    private Event createEvent(String eventId, String name, String channelId) {
        Event event = new Event(
                eventId,
                name,
                "description",
                "leader",
                Instant.ofEpochSecond(1_800_000_000L),
                channelId,
                null,
                "2",
                new ArrayList<>(),
                new Guild("guild-1", "guild"),
                new EmbedType(1L, "raid", "{}", null)
        );
        event.setStatus(EventStatus.OPEN);
        return event;
    }
}
