package com.github.havlli.EventPilot.entity.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventSignupServiceTest {

    private AutoCloseable autoCloseable;
    private EventSignupService underTest;
    @Mock
    private EventDAO eventDAO;
    @Mock
    private EmbedTypeService embedTypeService;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EventSignupService(eventDAO, embedTypeService);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void applySignup_AddsNewParticipant_WhenEventHasCapacity() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "2", new ArrayList<>());
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.ADDED);
        assertThat(event.getParticipants()).hasSize(1);
        Participant participant = event.getParticipants().get(0);
        assertThat(participant.getUserId()).isEqualTo("123");
        assertThat(participant.getRoleIndex()).isEqualTo(1);
        assertThat(participant.getPosition()).isEqualTo(1);
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.SIGNED_UP);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void applySignup_UpdatesExistingParticipant_WhenEventIsFull() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "1", new ArrayList<>());
        Participant participant = new Participant("123", "user", 1, 1, event);
        event.getParticipants().add(participant);
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 2);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.UPDATED);
        assertThat(event.getParticipants()).hasSize(1);
        assertThat(participant.getRoleIndex()).isEqualTo(2);
        assertThat(participant.getPosition()).isEqualTo(1);
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.SIGNED_UP);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void applySignup_WaitlistsNewPositiveParticipant_WhenEventIsFull() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "1", new ArrayList<>());
        event.getParticipants().add(new Participant("456", "other", 1, 1, event));
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.WAITLISTED);
        assertThat(event.getParticipants()).hasSize(2);
        Participant participant = event.getParticipants().get(1);
        assertThat(participant.getUserId()).isEqualTo("123");
        assertThat(participant.getRoleIndex()).isEqualTo(1);
        assertThat(participant.getPosition()).isEqualTo(2);
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.WAITLISTED);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void applySignup_AddsNewNonCapacityParticipant_WhenPositiveRolesAreFull() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "1", new ArrayList<>());
        event.getParticipants().add(new Participant("456", "other", 1, 1, event));
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", -1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.ADDED);
        assertThat(event.getParticipants()).hasSize(2);
        Participant participant = event.getParticipants().get(1);
        assertThat(participant.getRoleIndex()).isEqualTo(-1);
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.SIGNED_UP);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void applySignup_WaitlistsExistingNonCapacityParticipant_WhenSwitchingToPositiveRoleAndEventIsFull() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "1", new ArrayList<>());
        Participant confirmedParticipant = new Participant("456", "other", 1, 1, event);
        Participant absenceParticipant = new Participant("123", "user", 2, -1, event);
        event.getParticipants().addAll(List.of(confirmedParticipant, absenceParticipant));
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 2);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.WAITLISTED);
        assertThat(absenceParticipant.getRoleIndex()).isEqualTo(2);
        assertThat(absenceParticipant.getStatus()).isEqualTo(ParticipantStatus.WAITLISTED);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void applySignup_PromotesEarliestWaitlistedParticipant_WhenConfirmedParticipantStopsConsumingCapacity() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "1", new ArrayList<>());
        Participant confirmedParticipant = new Participant("123", "user", 1, 1, event);
        Participant firstWaitlisted = new Participant("456", "first", 2, 2, event);
        firstWaitlisted.setStatus(ParticipantStatus.WAITLISTED);
        Participant secondWaitlisted = new Participant("789", "second", 3, 1, event);
        secondWaitlisted.setStatus(ParticipantStatus.WAITLISTED);
        event.getParticipants().addAll(List.of(confirmedParticipant, firstWaitlisted, secondWaitlisted));
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", -1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.UPDATED);
        assertThat(confirmedParticipant.getRoleIndex()).isEqualTo(-1);
        assertThat(confirmedParticipant.getStatus()).isEqualTo(ParticipantStatus.SIGNED_UP);
        assertThat(firstWaitlisted.getStatus()).isEqualTo(ParticipantStatus.SIGNED_UP);
        assertThat(secondWaitlisted.getStatus()).isEqualTo(ParticipantStatus.WAITLISTED);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void applySignup_ReturnsEventNotFound_WhenEventDoesNotExist() {
        // Arrange
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.empty());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.EVENT_NOT_FOUND);
        verifyNoInteractions(embedTypeService);
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void applySignup_ReturnsRoleNotFound_WhenRoleIsNotDefined() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "2", new ArrayList<>());
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType()))
                .thenReturn(new HashMap<>(Map.of(1, "Tank")));

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 2);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.ROLE_NOT_FOUND);
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void applySignup_ReturnsInvalidCapacity_WhenMemberSizeIsMalformed() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "many", new ArrayList<>());
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.INVALID_CAPACITY);
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void applySignup_ReturnsEventClosed_WhenEventIsClosed() {
        // Arrange
        Event event = createEvent("10", "2", new ArrayList<>());
        event.setStatus(EventStatus.CLOSED);
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.EVENT_CLOSED);
        verifyNoInteractions(embedTypeService);
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void applySignup_ReturnsEventCancelled_WhenEventIsCancelled() {
        // Arrange
        Event event = createEvent("10", "2", new ArrayList<>());
        event.setStatus(EventStatus.CANCELLED);
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.EVENT_CANCELLED);
        verifyNoInteractions(embedTypeService);
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void applySignup_ReturnsEventExpired_WhenEventIsExpired() {
        // Arrange
        Event event = createEvent("10", "2", new ArrayList<>());
        event.setStatus(EventStatus.EXPIRED);
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 1);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.EVENT_EXPIRED);
        verifyNoInteractions(embedTypeService);
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void applySignup_DoesNotCreateNewParticipant_WhenDuplicateUserAlreadyExists() throws JsonProcessingException {
        // Arrange
        Event event = createEvent("10", "5", new ArrayList<>());
        Participant first = new Participant("123", "user", 1, 1, event);
        Participant duplicate = new Participant("123", "user", 2, 1, event);
        event.getParticipants().addAll(List.of(first, duplicate));
        when(eventDAO.findByIdForUpdate("10")).thenReturn(Optional.of(event));
        when(embedTypeService.getDeserializedMap(event.getEmbedType())).thenReturn(defaultRoles());

        // Act
        EventSignupResult actual = underTest.applySignup("10", "123", "user", 2);

        // Assert
        assertThat(actual.outcome()).isEqualTo(EventSignupResult.Outcome.UPDATED);
        assertThat(event.getParticipants()).hasSize(2);
        assertThat(first.getRoleIndex()).isEqualTo(2);
        assertThat(duplicate.getRoleIndex()).isEqualTo(2);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    private Event createEvent(String eventId, String memberSize, List<Participant> participants) {
        return new Event(
                eventId,
                "name",
                "description",
                "author",
                Instant.now().plusSeconds(3600),
                "123456",
                null,
                memberSize,
                participants,
                new Guild("1", "guild"),
                new EmbedType(1L, "default", "{}", null)
        );
    }

    private HashMap<Integer, String> defaultRoles() {
        return new HashMap<>(Map.of(
                1, "Tank",
                2, "Healer",
                -1, "Absence"
        ));
    }
}
