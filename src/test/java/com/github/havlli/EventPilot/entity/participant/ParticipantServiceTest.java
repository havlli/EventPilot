package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ParticipantServiceTest {

    @Mock
    private ParticipantDAO participantDAO;
    @InjectMocks
    private ParticipantService underTest;
    private AutoCloseable autoCloseable;

    @BeforeEach
    public void beforeEach() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void afterEach() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void getParticipant_ReturnsOptionalParticipant_WhenParticipantExists() {
        // Arrange
        Participant expected = new Participant("1","user1",1,1, new Event());
        Participant notExpected = new Participant("2","user2",1,1, new Event());
        List<Participant> participantList = List.of(expected, notExpected);

        // Act
        Optional<Participant> actual = underTest.getParticipant(expected.getUserId(), participantList);

        // Assert
        assertThat(actual).isPresent()
                .usingRecursiveComparison().isEqualTo(Optional.of(expected));
    }

    @Test
    public void getParticipant_ReturnsEmptyOptional_WhenParticipantNotExists() {
        // Arrange
        Participant notExpected = new Participant("2","user2",1,1, new Event());
        List<Participant> participantList = List.of(notExpected);
        String notExistingUserId = "1";

        // Act
        Optional<Participant> actual = underTest.getParticipant(notExistingUserId, participantList);

        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    public void addParticipant() {
        // Arrange
        Participant expected = new Participant("1","user1",1,1, new Event());
        List<Participant> expectedList = List.of(expected);

        List<Participant> actualList = new ArrayList<>();

        // Act
        underTest.addParticipant(expected, actualList);

        // Assert
        assertThat(actualList).isEqualTo(expectedList);
    }

    @Test
    public void updateParticipant() {
        // Arrange
        Participant oldParticipant = new Participant("1","user1",1,1, new Event());
        Integer roleIndex = 2;
        Participant expectedParticipant = new Participant("1","user1",1, roleIndex, new Event());

        // Act
        underTest.updateRoleIndex(oldParticipant, roleIndex);

        // Assert
        assertThat(oldParticipant).isEqualTo(expectedParticipant);
    }

    @Test
    public void getParticipantsByEvent() {
        // Arrange
        Event eventMock = mock(Event.class);

        // Act
        underTest.getParticipantsByEvent(eventMock);

        // Assert
        verify(participantDAO, times(1)).getParticipantsByEvent(eventMock);
    }
}