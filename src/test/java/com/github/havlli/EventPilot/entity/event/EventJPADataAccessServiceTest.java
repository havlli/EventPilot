package com.github.havlli.EventPilot.entity.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

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
        underTest.insertEvent(event);

        // Assert
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void fetchEvents() {
        // Act
        underTest.fetchEvents();

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
        underTest.fetchExpiredEvents();

        // Assert
        verify(eventRepository, times(1)).findAllWithDatetimeBeforeCurrentTime();
    }
}