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

    private Event createEvent() {
        Event event = new Event(
                "event-1",
                "Raid Night",
                "description",
                "leader",
                Instant.ofEpochSecond(1_800_000_000L),
                "channel-1",
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
