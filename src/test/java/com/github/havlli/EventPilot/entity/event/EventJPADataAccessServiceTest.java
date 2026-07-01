package com.github.havlli.EventPilot.entity.event;

import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventJPADataAccessServiceTest {

     private AutoCloseable autoCloseable;
     @Mock
     private EventRepository eventRepository;
     @InjectMocks
     private EventJPADataAccessService underTest;


    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void insertEvent() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        underTest.saveEvent(event);

        // Assert
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void fetchEvents() {
        // Act
        underTest.getEvents();

        // Assert
        verify(eventRepository, times(1)).findAll();
    }

    @Test
    void deleteEvent() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        underTest.deleteEvent(event);

        // Assert
        verify(eventRepository, times(1)).delete(event);
    }

    @Test
    void deleteAllEvents() {
        // Arrange
        Event event = mock(Event.class);
        List<Event> events = List.of(event);

        // Act
        underTest.deleteAllEvents(events);

        // Assert
        verify(eventRepository, times(1)).deleteAll(events);
    }

    @Test
    void fetchExpiredEvents() {
        // Act
        underTest.getExpiredEvents();

        // Assert
        verify(eventRepository, times(1)).findAllWithDatetimeBeforeCurrentTime();
    }

    @Test
    void fetchReminderCandidates() {
        // Arrange
        Instant reminderCutoff = Instant.now();

        // Act
        underTest.getReminderCandidates(reminderCutoff);

        // Assert
        verify(eventRepository, times(1)).findReminderCandidates(reminderCutoff);
    }

    @Test
    void deleteById() {
        // Arrange
        String eventId = "1234";

        // Act
        underTest.deleteById(eventId);

        // Assert
        verify(eventRepository, times(1)).deleteById(eventId);
    }

    @Test
    void existsById() {
        // Arrange
        String eventId = "1234";

        // Act
        underTest.existsById(eventId);

        // Assert
        verify(eventRepository, times(1)).existsById(eventId);
    }

    @Test
    void getEventsForGuild_DelegatesToAllGuildEvents_WhenStatusesAreNullAndClampsLimit() {
        // Arrange
        Event event = mock(Event.class);
        when(eventRepository.findByGuildIdOrderByDateTimeAsc("guild-1", PageRequest.of(0, 1)))
                .thenReturn(List.of(event));

        // Act
        List<Event> actual = underTest.getEventsForGuild("guild-1", null, 0);

        // Assert
        assertThat(actual).containsExactly(event);
        verify(eventRepository, times(1))
                .findByGuildIdOrderByDateTimeAsc("guild-1", PageRequest.of(0, 1));
        verify(eventRepository, never()).findByGuildIdAndStatusInOrderByDateTimeAsc(any(), any(), any());
    }

    @Test
    void getEventsForGuild_DelegatesToAllGuildEvents_WhenStatusesAreEmpty() {
        // Arrange
        Event event = mock(Event.class);
        when(eventRepository.findByGuildIdOrderByDateTimeAsc("guild-1", PageRequest.of(0, 5)))
                .thenReturn(List.of(event));

        // Act
        List<Event> actual = underTest.getEventsForGuild("guild-1", List.of(), 5);

        // Assert
        assertThat(actual).containsExactly(event);
        verify(eventRepository, times(1))
                .findByGuildIdOrderByDateTimeAsc("guild-1", PageRequest.of(0, 5));
        verify(eventRepository, never()).findByGuildIdAndStatusInOrderByDateTimeAsc(any(), any(), any());
    }

    @Test
    void getEventsForGuild_DelegatesToStatusFilteredGuildEvents() {
        // Arrange
        Event event = mock(Event.class);
        List<EventStatus> statuses = List.of(EventStatus.OPEN, EventStatus.CLOSED);
        when(eventRepository.findByGuildIdAndStatusInOrderByDateTimeAsc("guild-1", statuses, PageRequest.of(0, 5)))
                .thenReturn(List.of(event));

        // Act
        List<Event> actual = underTest.getEventsForGuild("guild-1", statuses, 5);

        // Assert
        assertThat(actual).containsExactly(event);
        verify(eventRepository, times(1))
                .findByGuildIdAndStatusInOrderByDateTimeAsc("guild-1", statuses, PageRequest.of(0, 5));
        verify(eventRepository, never()).findByGuildIdOrderByDateTimeAsc(any(), any());
    }

    @Test
    void findById() {
        // Arrange
        String eventId = "1234";

        // Act
        underTest.findById(eventId);

        // Assert
        verify(eventRepository, times(1)).findById(eventId);
    }

    @Test
    void findByIdAndGuildId() {
        // Arrange
        String eventId = "1234";

        // Act
        underTest.findByIdAndGuildId(eventId, "guild-1");

        // Assert
        verify(eventRepository, times(1)).findByIdAndGuildId(eventId, "guild-1");
    }

    @Test
    void findByIdForUpdate() {
        // Arrange
        String eventId = "1234";

        // Act
        underTest.findByIdForUpdate(eventId);

        // Assert
        verify(eventRepository, times(1)).findByIdForUpdate(eventId);
    }
}
