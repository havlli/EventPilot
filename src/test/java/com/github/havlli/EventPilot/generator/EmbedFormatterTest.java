package com.github.havlli.EventPilot.generator;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.participant.Participant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmbedFormatterTest {

    private EmbedFormatter underTest;
    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedFormatter();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void raidSize() {
        // Arrange
        int current = 5;
        int maximum = 10;
        String pattern = "%s/%s";
        String expected = String.format(pattern, current, maximum);

        // Act
        String actual = underTest.raidSize(current, maximum);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void leaderWithId() {
        // Arrange
        String leader = "leader";
        String id = "10";
        String pattern = "Leader: %s - ID: %s";
        String expected = String.format(pattern, leader, id);

        // Act
        String actual = underTest.leaderWithId(leader, id);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void date() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:D>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.date(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void time() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:t>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.time(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void dateTime() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:f>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.dateTime(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void relativeTime() {
        // Arrange
        Instant instant = Instant.now();
        long epochSeconds = instant.getEpochSecond();
        String pattern = "<t:%d:R>";
        String expected = String.format(pattern, epochSeconds);

        // Act
        String actual = underTest.relativeTime(instant);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createConcatField_ReturnsOneLinerConcatenatedString_WhenOneOrMoreMatchingUsers() {
        // Arrange
        Event eventMock = mock(Event.class);
        Participant participantOne = new Participant(1L,"123","userOne",1,1, eventMock);
        Participant participantTwo = new Participant(2L,"234","userTwo",2,1, eventMock);
        Participant participantThree = new Participant(3L,"345","userThree",3,1, eventMock);
        Participant participantFour = new Participant(4L,"456","userFour",4,1, eventMock);
        List<Participant> matchingUsers = List.of(participantOne, participantTwo, participantThree, participantFour);
        String fieldName = "testName";
        boolean isOneLineField = true;

        // Act
        String actual = underTest.createConcatField(fieldName, matchingUsers, isOneLineField);

        // Assert
        long numberOfBreaks = Arrays.stream(actual.split("")).filter(character -> character.equals("\n"))
                .count();
        List<String> expectedParticipants = matchingUsers.stream().map(Participant::getUsername).toList();

        assertThat(numberOfBreaks).isEqualTo(0L);
        assertThat(actual).contains(expectedParticipants);
    }

    @Test
    void createConcatField_ReturnsMultiLinerConcatenatedString_WhenOneOrMoreMatchingUsers() {
        // Arrange
        Event eventMock = mock(Event.class);
        Participant participantOne = new Participant(1L,"123","userOne",1,1, eventMock);
        Participant participantTwo = new Participant(2L,"234","userTwo",2,1, eventMock);
        Participant participantThree = new Participant(3L,"345","userThree",3,1, eventMock);
        Participant participantFour = new Participant(4L,"456","userFour",4,1, eventMock);
        List<Participant> matchingUsers = List.of(participantOne, participantTwo, participantThree, participantFour);
        String fieldName = "testName";
        boolean isOneLineField = false;

        // Act
        String actual = underTest.createConcatField(fieldName, matchingUsers, isOneLineField);

        // Assert
        long numberOfBreaks = Arrays.stream(actual.split("")).filter(character -> character.equals("\n"))
                .count();
        List<String> expectedParticipants = matchingUsers.stream().map(Participant::getUsername).toList();

        assertThat(numberOfBreaks).isEqualTo(4L);
        assertThat(actual).contains(expectedParticipants);
    }

    @Test
    void createConcatField_ReturnsOnlyOneLinerRoleNameString_WhenNoMatchingUsers() {
        // Arrange
        List<Participant> matchingUsers = List.of();
        String fieldName = "testName";
        boolean isOneLineField = true;

        // Act
        String actual = underTest.createConcatField(fieldName, matchingUsers, isOneLineField);

        // Assert
        long numberOfBreaks = Arrays.stream(actual.split("")).filter(character -> character.equals("\n"))
                .count();

        assertThat(numberOfBreaks).isEqualTo(0L);
        assertThat(actual).containsOnlyOnce(fieldName + " (0): ");
    }

    @Test
    void createConcatField_ReturnsMultiLinerRoleNameString_WhenNoMatchingUsers() {
        // Arrange
        List<Participant> matchingUsers = List.of();
        String fieldName = "testName";
        boolean isOneLineField = false;

        // Act
        String actual = underTest.createConcatField(fieldName, matchingUsers, isOneLineField);

        // Assert
        long numberOfBreaks = Arrays.stream(actual.split("")).filter(character -> character.equals("\n"))
                .count();

        assertThat(numberOfBreaks).isEqualTo(1L);
        assertThat(actual).contains(fieldName + " (0):");
    }
}