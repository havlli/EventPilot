package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ParticipantDTOTest {

    private Participant participant;

    @BeforeEach
    void setUp() {
        participant = new Participant(
                1L,
                "123",
                "test",
                1,
                1,
                mock(Event.class)
        );
    }

    @Test
    void fromParticipant() {
        // Arrange
        ParticipantDTO expected = new ParticipantDTO(
                participant.getId(),
                participant.getUserId(),
                participant.getUsername(),
                participant.getPosition(),
                participant.getRoleIndex()
        );

        // Act
        ParticipantDTO actual = ParticipantDTO.fromParticipant(participant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }
}