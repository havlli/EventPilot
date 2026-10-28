package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.core.DiscordProperties;
import com.github.havlli.EventPilot.entity.guild.Guild;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
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
    private EventService underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EventService(eventDAO, new EventSettingsResolver(new DiscordProperties(
                "test", null, new DiscordProperties.Scheduler(60, 60))));
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
    void getReminderCandidates_DelegatesWithLeadTimeCutoff() {
        // Arrange
        Duration reminderLeadTime = Duration.ofMinutes(60);
        Instant beforeCall = Instant.now();

        // Act
        underTest.getReminderCandidates();

        // Assert
        verify(eventDAO).getMaxReminderLeadMinutes();
        verify(eventDAO, times(1)).getReminderCandidates(argThat(cutoff ->
                !cutoff.isBefore(beforeCall.plus(reminderLeadTime))
                        && !cutoff.isAfter(Instant.now().plus(reminderLeadTime))
        ));
    }

    @Test
    void getReminderCandidates_FiltersEachResolvedLeadAndKeepsInclusiveBoundary() {
        Instant now = Instant.parse("2026-10-28T12:00:00Z");
        Event shorter = reminderEvent("short", now.plusSeconds(30 * 60), 10);
        Event shorterDue = reminderEvent("short-due", now.plusSeconds(10 * 60), 10);
        Event longer = reminderEvent("long", now.plusSeconds(90 * 60), 120);
        Event inherited = reminderEvent("inherited", now.plusSeconds(60 * 60), null);
        Event beyondDefault = reminderEvent("later", now.plusSeconds(60 * 60).plusNanos(1), null);
        Event beyondOverride = reminderEvent("later-override", now.plusSeconds(120 * 60).plusNanos(1), 120);
        when(eventDAO.getMaxReminderLeadMinutes()).thenReturn(120);
        when(eventDAO.getReminderCandidates(now.plusSeconds(120 * 60)))
                .thenReturn(List.of(shorter, shorterDue, longer, inherited, beyondDefault, beyondOverride));

        assertThat(underTest.getReminderCandidates(now)).containsExactly(shorterDue, longer, inherited);
        verify(eventDAO).getMaxReminderLeadMinutes();
        verify(eventDAO).getReminderCandidates(now.plusSeconds(120 * 60));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {10})
    void getReminderCandidates_UsesInstanceLeadWhenMaximumOverrideIsMissingOrShorter(Integer maximum) {
        Instant now = Instant.parse("2026-10-28T12:00:00Z");
        underTest = new EventService(eventDAO, new EventSettingsResolver(new DiscordProperties(
                "test", null, new DiscordProperties.Scheduler(60, 90))));
        Event inherited = reminderEvent("inherited", now.plusSeconds(90 * 60), null);
        when(eventDAO.getMaxReminderLeadMinutes()).thenReturn(maximum);
        when(eventDAO.getReminderCandidates(now.plusSeconds(90 * 60))).thenReturn(List.of(inherited));

        assertThat(underTest.getReminderCandidates(now)).containsExactly(inherited);
        verify(eventDAO).getReminderCandidates(now.plusSeconds(90 * 60));
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"OPEN", "CLOSED"})
    void updateReminderLeadMinutesForGuild_SavesOverrideUnderLock(EventStatus status) {
        Event event = createEvent("1", createGuild(), createEmbedType());
        event.setStatus(status);
        event.setReminderSent(true);
        when(eventDAO.findByIdForUpdate("1")).thenReturn(Optional.of(event));

        assertThat(underTest.updateReminderLeadMinutesForGuild("1", "1", 10080)).contains(event);
        assertThat(event.getReminderLeadMinutes()).isEqualTo(10080);
        assertThat(event.isReminderSent()).isTrue();
        verify(eventDAO).saveEvent(event);
        verify(eventDAO, never()).findById(any());
    }

    @Test
    void updateReminderLeadMinutesForGuild_ResetsToNull() {
        Event event = createEvent("1", createGuild(), createEmbedType());
        event.setReminderLeadMinutes(120);
        when(eventDAO.findByIdForUpdate("1")).thenReturn(Optional.of(event));

        assertThat(underTest.updateReminderLeadMinutesForGuild("1", "1", null)).contains(event);
        assertThat(event.getReminderLeadMinutes()).isNull();
        verify(eventDAO).saveEvent(event);
    }

    @Test
    void updateReminderLeadMinutesForGuild_RejectsWrongGuildWithoutMutation() {
        Event event = createEvent("1", createGuild(), createEmbedType());
        event.setReminderLeadMinutes(120);
        when(eventDAO.findByIdForUpdate("1")).thenReturn(Optional.of(event));

        assertThat(underTest.updateReminderLeadMinutesForGuild("1", "other", 60)).isEmpty();
        assertThat(event.getReminderLeadMinutes()).isEqualTo(120);
        verify(eventDAO, never()).saveEvent(any());
    }

    @ParameterizedTest
    @EnumSource(value = EventStatus.class, names = {"CANCELLED", "EXPIRED"})
    void updateReminderLeadMinutesForGuild_RejectsInactiveStatus(EventStatus status) {
        Event event = createEvent("1", createGuild(), createEmbedType());
        event.setStatus(status);
        when(eventDAO.findByIdForUpdate("1")).thenReturn(Optional.of(event));

        assertThat(underTest.updateReminderLeadMinutesForGuild("1", "1", 60)).isEmpty();
        assertThat(event.getReminderLeadMinutes()).isNull();
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void updateReminderLeadMinutesForGuild_ReturnsEmptyForMissingEvent() {
        when(eventDAO.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThat(underTest.updateReminderLeadMinutesForGuild("missing", "1", 1)).isEmpty();
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void updateReminderLeadMinutesForGuild_RejectsEventWithoutGuild() {
        Event event = createEvent("1", null, createEmbedType());
        when(eventDAO.findByIdForUpdate("1")).thenReturn(Optional.of(event));

        assertThat(underTest.updateReminderLeadMinutesForGuild("1", "1", 60)).isEmpty();
        verify(eventDAO, never()).saveEvent(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10081, Integer.MAX_VALUE})
    void updateReminderLeadMinutesForGuild_RejectsInvalidRangeBeforeLookup(int minutes) {
        assertThatThrownBy(() -> underTest.updateReminderLeadMinutesForGuild("1", "1", minutes))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(eventDAO);
    }

    private Event reminderEvent(String id, Instant start, Integer lead) {
        return Event.builder().withEventId(id).withDateTime(start).withReminderLeadMinutes(lead).build();
    }

    @Test
    void updateStatusIfExists_UpdatesStatusAndReturnsEvent_WhenEventExists() {
        // Arrange
        var eventId = "1";
        Event event = createEvent(eventId, createGuild(), createEmbedType());
        when(eventDAO.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        Optional<Event> actual = underTest.updateStatusIfExists(eventId, EventStatus.CLOSED);

        // Assert
        assertThat(actual).contains(event);
        assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void updateStatusIfExists_ReturnsEmpty_WhenEventDoesNotExist() {
        // Arrange
        var eventId = "1";
        when(eventDAO.findById(eventId)).thenReturn(Optional.empty());

        // Act
        Optional<Event> actual = underTest.updateStatusIfExists(eventId, EventStatus.CLOSED);

        // Assert
        assertThat(actual).isEmpty();
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void markExpiredIfExists_ReturnsTrueAndMarksExpired_WhenEventExists() {
        // Arrange
        var eventId = "1";
        Event event = createEvent(eventId, createGuild(), createEmbedType());
        when(eventDAO.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        boolean actual = underTest.markExpiredIfExists(eventId);

        // Assert
        assertThat(actual).isTrue();
        assertThat(event.getStatus()).isEqualTo(EventStatus.EXPIRED);
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void markExpiredIfExists_ReturnsFalse_WhenEventDoesNotExist() {
        // Arrange
        var eventId = "1";
        when(eventDAO.findById(eventId)).thenReturn(Optional.empty());

        // Act
        boolean actual = underTest.markExpiredIfExists(eventId);

        // Assert
        assertThat(actual).isFalse();
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void markReminderSentIfExists_ReturnsTrueAndMarksReminderSent_WhenEventExists() {
        // Arrange
        var eventId = "1";
        Event event = createEvent(eventId, createGuild(), createEmbedType());
        when(eventDAO.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        boolean actual = underTest.markReminderSentIfExists(eventId);

        // Assert
        assertThat(actual).isTrue();
        assertThat(event.isReminderSent()).isTrue();
        verify(eventDAO, times(1)).saveEvent(event);
    }

    @Test
    void markReminderSentIfExists_ReturnsFalse_WhenEventDoesNotExist() {
        // Arrange
        var eventId = "1";
        when(eventDAO.findById(eventId)).thenReturn(Optional.empty());

        // Act
        boolean actual = underTest.markReminderSentIfExists(eventId);

        // Assert
        assertThat(actual).isFalse();
        verify(eventDAO, never()).saveEvent(any());
    }

    @Test
    void getEventsForGuild_DelegatesToDao() {
        // Arrange
        List<EventStatus> statuses = List.of(EventStatus.OPEN, EventStatus.CLOSED);
        List<Event> expected = List.of(mock(Event.class));
        when(eventDAO.getEventsForGuild("guild-1", statuses, 5)).thenReturn(expected);

        // Act
        List<Event> actual = underTest.getEventsForGuild("guild-1", statuses, 5);

        // Assert
        assertThat(actual).isEqualTo(expected);
        verify(eventDAO, times(1)).getEventsForGuild("guild-1", statuses, 5);
    }

    @Test
    void getEventByIdForGuild_DelegatesToDao() {
        // Arrange
        Event expected = mock(Event.class);
        when(eventDAO.findByIdAndGuildId("event-1", "guild-1")).thenReturn(Optional.of(expected));

        // Act
        Optional<Event> actual = underTest.getEventByIdForGuild("event-1", "guild-1");

        // Assert
        assertThat(actual).contains(expected);
        verify(eventDAO, times(1)).findByIdAndGuildId("event-1", "guild-1");
    }

    @Test
    void deleteEventIfExists_ReturnsTrueAndDeletesEvent_WhenEventExists() {
        // Arrange
        var eventId = "1";
        when(eventDAO.existsById(eventId)).thenReturn(true);

        // Act
        boolean actual = underTest.deleteEventIfExists(eventId);

        // Assert
        assertThat(actual).isTrue();
        verify(eventDAO, times(1)).deleteById(eventId);
    }

    @Test
    void deleteEventIfExists_ReturnsFalse_WhenEventDoesNotExist() {
        // Arrange
        var eventId = "1";
        when(eventDAO.existsById(eventId)).thenReturn(false);

        // Act
        boolean actual = underTest.deleteEventIfExists(eventId);

        // Assert
        assertThat(actual).isFalse();
        verify(eventDAO, never()).deleteById(eventId);
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
