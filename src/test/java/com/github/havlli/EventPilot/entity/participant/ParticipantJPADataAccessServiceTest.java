package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class ParticipantJPADataAccessServiceTest {

    private AutoCloseable autoCloseable;
    @Mock
    private ParticipantRepository participantRepository;
    @InjectMocks
    private ParticipantJPADataAccessService underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void getAllParticipantsByEvent() {
        // Arrange
        Event event = mock(Event.class);

        // Act
        underTest.getParticipantsByEvent(event);

        // Assert
        verify(participantRepository, times(1)).findAllByEvent(event);
    }

    @Test
    void saveParticipant() {
        // Arrange
        Participant participant = mock(Participant.class);

        // Act
        underTest.saveParticipant(participant);

        // Assert
        verify(participantRepository, times(1)).save(participant);
    }

    @Test
    void findById() {
        // Arrange
        Long id = 1L;

        // Act
        underTest.findById(id);

        // Assert
        verify(participantRepository, times(1)).findById(id);
    }
}