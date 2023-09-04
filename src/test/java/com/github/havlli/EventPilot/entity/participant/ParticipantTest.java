package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParticipantTest {

    private AutoCloseable autoCloseable;
    private Participant underTest;
    @Mock
    private Event eventMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new Participant(1L, "12345", "username", 1, 1, eventMock);
    }

    @Test
    void Participant_toString() {
        // Arrange
        when(eventMock.getEventId()).thenReturn("12345");
        String expected = "Participant{id=1, userId='12345', username='username', position=1, roleIndex=1, event=12345}";

        // Act
        String actual = underTest.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}