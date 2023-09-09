package com.github.havlli.EventPilot.entity.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

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
}