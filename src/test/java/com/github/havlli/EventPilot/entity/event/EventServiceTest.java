package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EventServiceTest {

    private AutoCloseable autoCloseable;
    @Mock
    private EventDAO eventDAO;
    @InjectMocks
    private EventService underTest;


    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void saveEvent() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        underTest.saveEvent(event);

        // Assert
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void getAllEvents() {
        // Act
        underTest.getAllEvents();

        // Assert
        verify(eventDAO, times(1)).getEvents();
    }

    @Test
    void deleteEvent() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        underTest.deleteEvent(event);

        // Assert
        verify(eventDAO, times(1)).deleteEvent(event);
    }

    @Test
    void deleteAllEvents() {
        // Arrange
        Event event = mock(Event.class);
        List<Event> eventList = List.of(event);

        // Act
        underTest.deleteAllEvents(eventList);

        // Assert
        verify(eventDAO, times(1)).deleteAllEvents(eventList);
    }

    @Test
    void getExpiredEvents() {
        // Act
        underTest.getExpiredEvents();

        // Assert
        verify(eventDAO, times(1)).getExpiredEvents();
    }

    @Test
    void deleteEventById_deletesEvent_WhenEventExists() {
        // Arrange
        var eventId = "1";
        when(eventDAO.existsById(eventId)).thenReturn(true);

        // Act
        underTest.deleteEventById(eventId);

        // Assert
        verify(eventDAO, times(1)).deleteById(eventId);
    }

    @Test
    void deleteEventById_throwsException_WhenEventNotExists() {
        // Arrange
        var eventId = "1";
        when(eventDAO.existsById(eventId)).thenReturn(false);

        // Assert
        assertThatThrownBy(() -> underTest.deleteEventById(eventId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventDAO, never()).deleteById(eventId);
    }

    @Test
    void updateEvent_updatesEventAndReturnsUpdatedEvent_WhenEventExists() {
        // Arrange
        var eventId = "1";

        Guild guild = createGuild();
        EmbedType embedType = createEmbedType();

        Event event = createEvent(eventId, guild, embedType);

        EventUpdateRequest updateRequest = new EventUpdateRequest(
                "newName",  "newDescription", "5"
        );

        when(eventDAO.findById(eventId)).thenReturn(Optional.of(event));
        doNothing().when(eventDAO).saveEvent(event);

        // Act
        Event actual = underTest.updateEvent(eventId, updateRequest);

        // Assert
        assertThat(actual).hasFieldOrPropertyWithValue("name", "newName");
        assertThat(actual).hasFieldOrPropertyWithValue("description", "newDescription");
        assertThat(actual).hasFieldOrPropertyWithValue("memberSize", "5");
        verify(eventDAO, times(1)).saveEvent(any());
    }

    @Test
    void updateEvent_ThrowsException_WhenEventNotFound() {
        // Arrange
        var eventId = "1";

        EventUpdateRequest updateRequest = new EventUpdateRequest(
                "newName",  "newDescription", "5"
        );

        when(eventDAO.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> underTest.updateEvent(eventId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Cannot update event with id");
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void updateEvent_updatesEventAndReturnsUpdatedEvent_WhenEventExistsAndNameIsNull() {
        // Arrange
        var eventId = "1";

        Guild guild = createGuild();
        EmbedType embedType = createEmbedType();

        Event event = createEvent(eventId, guild, embedType);

        EventUpdateRequest updateRequest = new EventUpdateRequest(
                null,  "newDescription", null
        );

        when(eventDAO.findById(eventId)).thenReturn(Optional.of(event));
        doNothing().when(eventDAO).saveEvent(event);

        // Act
        Event actual = underTest.updateEvent(eventId, updateRequest);

        // Assert
        assertThat(actual).hasFieldOrPropertyWithValue("name", event.getName());
        assertThat(actual).hasFieldOrPropertyWithValue("description", "newDescription");
        assertThat(actual).hasFieldOrPropertyWithValue("memberSize", event.getMemberSize());
        verify(eventDAO, times(1)).saveEvent(any());
    }


    // Helper Methods
    @NotNull
    private static Event createEvent(String eventId, Guild guild, EmbedType embedType) {
        return new Event(
                eventId,
                "existing",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
    }

    @NotNull
    private static EmbedType createEmbedType() {
        return new EmbedType(
                1L,
                "test",
                "test",
                null
        );
    }

    @NotNull
    private static Guild createGuild() {
        return new Guild("1", "guild");
    }
}