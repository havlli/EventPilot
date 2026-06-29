package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.TimeTester;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeRepository;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EventRepositoryIT extends TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(EventRepositoryIT.class);
    private static final int TIMESTAMP_CLOCK_SKEW_TOLERANCE_SECONDS = 2;
    @Autowired
    private GuildRepository guildRepository;
    @Autowired
    private EmbedTypeRepository embedTypeRepository;
    @Autowired
    private EventRepository underTest;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TestEventJPARepository testRepository;
    private TimeTester timeTester;

    @BeforeEach
    public void setUp() {
        LOG.info("Number of beans initialized { {} }", applicationContext.getBeanDefinitionCount());
        timeTester = new TimeTester(entityManager.getEntityManager(), postgresSQLContainer);
    }

    @Test
    public void jpaQueryCurrentTimestamp_SatisfiesExecutionTimeTolerance() throws SQLException {
        // Arrange
        Instant beforeQuery = Instant.now();

        // Act
        Instant actualJdbcQuery = timeTester.getCurrentTimestampUsingJdbc();
        Instant actualNativeQuery = timeTester.getCurrentTimestampUsingPersistence();
        Instant actualJPQLQuery = testRepository.selectCurrentTimestamp();
        Instant afterQuery = Instant.now();

        // Assert
        assertTimestampWithinQueryWindow(actualNativeQuery, beforeQuery, afterQuery);
        assertTimestampWithinQueryWindow(actualJPQLQuery, beforeQuery, afterQuery);
        assertTimestampWithinQueryWindow(actualJdbcQuery, beforeQuery, afterQuery);
    }

    private static void assertTimestampWithinQueryWindow(Instant actual, Instant beforeQuery, Instant afterQuery) {
        assertThat(actual).isAfterOrEqualTo(beforeQuery.minus(TIMESTAMP_CLOCK_SKEW_TOLERANCE_SECONDS, ChronoUnit.SECONDS));
        assertThat(actual).isBeforeOrEqualTo(afterQuery.plus(TIMESTAMP_CLOCK_SKEW_TOLERANCE_SECONDS, ChronoUnit.SECONDS));
    }

    @Test
    public void findAllWithDatetimeBeforeCurrentTime_ReturnsListOfExpiredEvents_WhenOffsetOneMinute() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(1, ChronoUnit.MINUTES);
        Instant expiredEvent1DateTime = instantNow.minus(1, ChronoUnit.MINUTES);
        Instant expiredEvent2DateTime = instantNow.minus(1, ChronoUnit.MINUTES);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );

        Event validEvent = new Event(
                "123",
                "event1",
                "description",
                "12345",
                validDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(validEvent);
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild, embedType);

        Event expiredEvent1 = new Event(
                "456",
                "event2",
                "description",
                "12345",
                expiredEvent1DateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(expiredEvent1);
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild, embedType);

        Event expiredEvent2 = new Event(
                "789",
                "event3",
                "description",
                "12345",
                expiredEvent2DateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(expiredEvent2);
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild, embedType);

        // Act
        List<Event> actual = underTest.findAllWithDatetimeBeforeCurrentTime();

        assertThat(actual).containsOnly(fetchedExpiredEvent1, fetchedExpiredEvent2);
        assertThat(actual).doesNotContain(fetchedValidEvent);
    }

    @Test
    public void findAllWithDatetimeBeforeCurrentTime_ReturnsListOfExpiredEvents_WhenOffsetThreeSeconds() throws SQLException {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(3, ChronoUnit.SECONDS);
        Instant expiredEvent1DateTime = instantNow.minus(3, ChronoUnit.SECONDS);
        Instant expiredEvent2DateTime = instantNow.minus(3, ChronoUnit.SECONDS);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );

        Event validEvent = new Event(
                "123",
                "event1",
                "description",
                "12345",
                validDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(validEvent);
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild, embedType);

        Event expiredEvent1 = new Event(
                "456",
                "event2",
                "description",
                "12345",
                expiredEvent1DateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(expiredEvent1);
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild, embedType);

        Event expiredEvent2 = new Event(
                "789",
                "event3",
                "description",
                "12345",
                expiredEvent2DateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(expiredEvent2);
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild, embedType);

        // Act
        List<Event> actual = underTest.findAllWithDatetimeBeforeCurrentTime();

        assertThat(actual).containsOnly(fetchedExpiredEvent1, fetchedExpiredEvent2);
        assertThat(actual).doesNotContain(fetchedValidEvent);
    }

    @Test
    public void findAllWithDatetimeBeforeCurrentTime_ReturnsListOfExpiredEvents_WhenOffsetTwoSeconds() throws SQLException {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(2, ChronoUnit.SECONDS);
        Instant expiredEvent1DateTime = instantNow.minus(2, ChronoUnit.SECONDS);
        Instant expiredEvent2DateTime = instantNow.minus(2, ChronoUnit.SECONDS);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );

        Event validEvent = new Event(
                "123",
                "event1",
                "description",
                "12345",
                validDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(validEvent);
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild, embedType);

        Event expiredEvent1 = new Event(
                "456",
                "event2",
                "description",
                "12345",
                expiredEvent1DateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(expiredEvent1);
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild, embedType);

        Event expiredEvent2 = new Event(
                "789",
                "event3",
                "description",
                "12345",
                expiredEvent2DateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        events.add(expiredEvent2);
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild, embedType);

        // Act
        List<Event> actual = underTest.findAllWithDatetimeBeforeCurrentTime();
        assertThat(actual).containsOnly(fetchedExpiredEvent1, fetchedExpiredEvent2);
        assertThat(actual).doesNotContain(fetchedValidEvent);
    }

    @Test
    public void findAllWithDatetimeBeforeCurrentTime_ReturnsOnlyOpenAndClosedExpiredEvents() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Guild guild = new Guild("1", "guild");
        Instant expiredDateTime = instantNow.minus(1, ChronoUnit.MINUTES);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );

        Event openEvent = new Event(
                "111",
                "open",
                "description",
                "12345",
                expiredDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        openEvent.setStatus(EventStatus.OPEN);
        Event fetchedOpenEvent = saveEventToDatabase(openEvent, guild, embedType);

        Event closedEvent = new Event(
                "222",
                "closed",
                "description",
                "12345",
                expiredDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        closedEvent.setStatus(EventStatus.CLOSED);
        Event fetchedClosedEvent = saveEventToDatabase(closedEvent, guild, embedType);

        Event cancelledEvent = new Event(
                "333",
                "cancelled",
                "description",
                "12345",
                expiredDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        cancelledEvent.setStatus(EventStatus.CANCELLED);
        Event fetchedCancelledEvent = saveEventToDatabase(cancelledEvent, guild, embedType);

        Event expiredEvent = new Event(
                "444",
                "expired",
                "description",
                "12345",
                expiredDateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        expiredEvent.setStatus(EventStatus.EXPIRED);
        Event fetchedExpiredEvent = saveEventToDatabase(expiredEvent, guild, embedType);

        // Act
        List<Event> actual = underTest.findAllWithDatetimeBeforeCurrentTime();

        // Assert
        assertThat(actual).containsOnly(fetchedOpenEvent, fetchedClosedEvent);
        assertThat(actual).doesNotContain(fetchedCancelledEvent, fetchedExpiredEvent);
    }

    @Test
    public void findReminderCandidates_ReturnsOpenAndClosedUnremindedFutureEventsInsideCutoff() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Instant reminderCutoff = instantNow.plus(60, ChronoUnit.MINUTES);

        Guild guild = new Guild("1", "guild");
        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );

        Event openDueEvent = createRepositoryEvent("123", "open-due", instantNow.plus(30, ChronoUnit.MINUTES), guild, embedType);
        Event fetchedOpenDueEvent = saveEventToDatabase(openDueEvent, guild, embedType);

        Event closedDueEvent = createRepositoryEvent("456", "closed-due", instantNow.plus(45, ChronoUnit.MINUTES), guild, embedType);
        closedDueEvent.setStatus(EventStatus.CLOSED);
        Event fetchedClosedDueEvent = saveEventToDatabase(closedDueEvent, guild, embedType);

        Event alreadyRemindedEvent = createRepositoryEvent("789", "already-reminded", instantNow.plus(30, ChronoUnit.MINUTES), guild, embedType);
        alreadyRemindedEvent.setReminderSent(true);
        Event fetchedAlreadyRemindedEvent = saveEventToDatabase(alreadyRemindedEvent, guild, embedType);

        Event cancelledEvent = createRepositoryEvent("101", "cancelled", instantNow.plus(30, ChronoUnit.MINUTES), guild, embedType);
        cancelledEvent.setStatus(EventStatus.CANCELLED);
        Event fetchedCancelledEvent = saveEventToDatabase(cancelledEvent, guild, embedType);

        Event expiredStatusEvent = createRepositoryEvent("102", "expired-status", instantNow.plus(30, ChronoUnit.MINUTES), guild, embedType);
        expiredStatusEvent.setStatus(EventStatus.EXPIRED);
        Event fetchedExpiredStatusEvent = saveEventToDatabase(expiredStatusEvent, guild, embedType);

        Event futureOutsideCutoffEvent = createRepositoryEvent("103", "future-outside-cutoff", instantNow.plus(2, ChronoUnit.HOURS), guild, embedType);
        Event fetchedFutureOutsideCutoffEvent = saveEventToDatabase(futureOutsideCutoffEvent, guild, embedType);

        Event pastEvent = createRepositoryEvent("104", "past", instantNow.minus(1, ChronoUnit.MINUTES), guild, embedType);
        Event fetchedPastEvent = saveEventToDatabase(pastEvent, guild, embedType);

        // Act
        List<Event> actual = underTest.findReminderCandidates(reminderCutoff);

        // Assert
        assertThat(actual).containsOnly(fetchedOpenDueEvent, fetchedClosedDueEvent);
        assertThat(actual).doesNotContain(
                fetchedAlreadyRemindedEvent,
                fetchedCancelledEvent,
                fetchedExpiredStatusEvent,
                fetchedFutureOutsideCutoffEvent,
                fetchedPastEvent
        );
    }

    @Test
    public void findByGuildIdAndStatusInOrderByDateTimeAsc_ReturnsGuildScopedStatusFilteredEvents() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Guild targetGuild = new Guild("guild-1", "target");
        Guild otherGuild = new Guild("guild-2", "other");
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );

        Event laterOpenEvent = createRepositoryEvent("300", "later-open", instantNow.plus(30, ChronoUnit.MINUTES), targetGuild, embedType);
        Event fetchedLaterOpenEvent = saveEventToDatabase(laterOpenEvent, targetGuild, embedType);

        Event earlierClosedEvent = createRepositoryEvent("100", "earlier-closed", instantNow.plus(10, ChronoUnit.MINUTES), targetGuild, embedType);
        earlierClosedEvent.setStatus(EventStatus.CLOSED);
        Event fetchedEarlierClosedEvent = saveEventToDatabase(earlierClosedEvent, targetGuild, embedType);

        Event cancelledEvent = createRepositoryEvent("200", "cancelled", instantNow.plus(20, ChronoUnit.MINUTES), targetGuild, embedType);
        cancelledEvent.setStatus(EventStatus.CANCELLED);
        Event fetchedCancelledEvent = saveEventToDatabase(cancelledEvent, targetGuild, embedType);

        Event otherGuildEvent = createRepositoryEvent("400", "other-guild", instantNow.plus(5, ChronoUnit.MINUTES), otherGuild, embedType);
        Event fetchedOtherGuildEvent = saveEventToDatabase(otherGuildEvent, otherGuild, embedType);

        // Act
        List<Event> actual = underTest.findByGuildIdAndStatusInOrderByDateTimeAsc(
                "guild-1",
                List.of(EventStatus.OPEN, EventStatus.CLOSED),
                PageRequest.of(0, 10)
        );

        // Assert
        assertThat(actual).containsExactly(fetchedEarlierClosedEvent, fetchedLaterOpenEvent);
        assertThat(actual).doesNotContain(fetchedCancelledEvent, fetchedOtherGuildEvent);
    }

    @Test
    public void findByGuildIdOrderByDateTimeAsc_ReturnsLimitedGuildScopedEvents() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Guild targetGuild = new Guild("guild-1", "target");
        Guild otherGuild = new Guild("guild-2", "other");
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );

        Event firstEvent = createRepositoryEvent("100", "first", instantNow.plus(10, ChronoUnit.MINUTES), targetGuild, embedType);
        Event fetchedFirstEvent = saveEventToDatabase(firstEvent, targetGuild, embedType);

        Event secondEvent = createRepositoryEvent("200", "second", instantNow.plus(20, ChronoUnit.MINUTES), targetGuild, embedType);
        Event fetchedSecondEvent = saveEventToDatabase(secondEvent, targetGuild, embedType);

        Event thirdEvent = createRepositoryEvent("300", "third", instantNow.plus(30, ChronoUnit.MINUTES), targetGuild, embedType);
        Event fetchedThirdEvent = saveEventToDatabase(thirdEvent, targetGuild, embedType);

        Event otherGuildEvent = createRepositoryEvent("400", "other-guild", instantNow.plus(5, ChronoUnit.MINUTES), otherGuild, embedType);
        Event fetchedOtherGuildEvent = saveEventToDatabase(otherGuildEvent, otherGuild, embedType);

        // Act
        List<Event> actual = underTest.findByGuildIdOrderByDateTimeAsc(
                "guild-1",
                PageRequest.of(0, 2)
        );

        // Assert
        assertThat(actual).containsExactly(fetchedFirstEvent, fetchedSecondEvent);
        assertThat(actual).doesNotContain(fetchedThirdEvent, fetchedOtherGuildEvent);
    }

    @Test
    public void findByIdAndGuildId_ReturnsOnlyMatchingGuildEvent() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Guild targetGuild = new Guild("guild-1", "target");
        Guild otherGuild = new Guild("guild-2", "other");
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );

        Event targetEvent = createRepositoryEvent("100", "target", instantNow.plus(10, ChronoUnit.MINUTES), targetGuild, embedType);
        Event fetchedTargetEvent = saveEventToDatabase(targetEvent, targetGuild, embedType);

        Event otherGuildEvent = createRepositoryEvent("200", "other-guild", instantNow.plus(10, ChronoUnit.MINUTES), otherGuild, embedType);
        saveEventToDatabase(otherGuildEvent, otherGuild, embedType);

        // Act
        Optional<Event> actual = underTest.findByIdAndGuildId("100", "guild-1");
        Optional<Event> wrongGuild = underTest.findByIdAndGuildId("100", "guild-2");

        // Assert
        assertThat(actual).contains(fetchedTargetEvent);
        assertThat(wrongGuild).isEmpty();
    }

    @Test
    public void insertEvent_SavesEventToDatabase_WhenGuildExistsAndIgnoresTransientFields() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event expected = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        // Act
        underTest.save(expected);

        // Assert
        Optional<Event> actual = underTest.findById(expected.getEventId());

        assertThat(actual).isPresent()
                .hasValueSatisfying(e -> {
                    assertThat(e.getEventId()).isEqualTo(expected.getEventId());
                    assertThat(e.getName()).isEqualTo(expected.getName());
                    assertThat(e.getDescription()).isEqualTo(expected.getDescription());
                    assertThat(e.getAuthor()).isEqualTo(expected.getAuthor());
                    assertThat(e.getDateTime()).isEqualTo(expected.getDateTime());
                    assertThat(e.getDestinationChannelId()).isEqualTo(expected.getDestinationChannelId());
                    assertThat(e.getMemberSize()).isEqualTo(expected.getMemberSize());
                    assertThat(e.getStatus()).isEqualTo(expected.getStatus());
                    assertThat(e.getInstances()).isEqualTo(expected.getInstances());
                    assertThat(e.getParticipants()).usingRecursiveComparison().isEqualTo(expected.getParticipants());
                });
    }

    @Test
    public void fetchEvents_ReturnsListOfAllEvents() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event expectedEvent1 = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        Event expectedEvent2 = new Event(
                "234",
                "event2",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        underTest.save(expectedEvent1);
        underTest.save(expectedEvent2);

        // Act
        List<Event> actualEvents = underTest.findAll();

        // Assert
        assertThat(actualEvents).isNotEmpty();
        assertThat(actualEvents).hasSize(2);
        List<Event> expectedEvents = List.of(expectedEvent1, expectedEvent2);
        assertThat(actualEvents)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expectedEvents);
    }

    @Test
    public void fetchEvents_ReturnsEmptyList_WhenNoEventsExists() {
        // Act
        List<Event> actualEvents = underTest.findAll();

        // Assert
        assertThat(actualEvents).isEmpty();
        assertThat(actualEvents).hasSize(0);
    }

    @Test
    public void deleteEvent_DeletesEvent_WhenEventExists() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event expectedEvent = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        underTest.save(expectedEvent);

        // Act
        underTest.delete(expectedEvent);

        // Assert
        List<Event> actualEvents = underTest.findAll();
        assertThat(actualEvents).isEmpty();
        assertThat(actualEvents).hasSize(0);
    }

    @Test
    public void deleteEvent_DeletesNothing_WhenEventNotExists() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event existingEvent = new Event(
                "123",
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
        underTest.save(existingEvent);

        Event notExistingEvent = new Event(
                "456",
                "not-existing",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        // Act
        underTest.delete(notExistingEvent);

        // Assert
        List<Event> actualEvents = underTest.findAll();
        assertThat(actualEvents).hasSize(1);
        List<Event> expectedList = List.of(existingEvent);
        assertThat(actualEvents).usingRecursiveComparison()
                .isEqualTo(expectedList);
    }

    @Test
    public void deleteAllEvent_DeletesAllEvents_WhenAllEventsExists() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event expectedEvent1 = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(expectedEvent1);

        Event expectedEvent2 = new Event(
                "456",
                "event2",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(expectedEvent2);

        List<Event> expectedEvents = List.of(expectedEvent1,expectedEvent2);
        // Act
        underTest.deleteAll(expectedEvents);

        // Assert
        List<Event> actualEvents = underTest.findAll();
        assertThat(actualEvents).hasSize(0);
        assertThat(actualEvents).usingRecursiveComparison()
                .isNotEqualTo(expectedEvents);
    }

    @Test
    public void deleteAllEvent_DeletesAllEvents_WhenPartialEventsExists() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event expectedEvent1 = new Event(
                "123",
                "event1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(expectedEvent1);

        Event expectedEvent2 = new Event(
                "456",
                "event2",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(expectedEvent2);

        Event notExpectedEvent = new Event(
                "789",
                "event3",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        List<Event> passedEvents = List.of(expectedEvent1,expectedEvent2, notExpectedEvent);
        // Act
        underTest.deleteAll(passedEvents);

        // Assert
        List<Event> actualEvents = underTest.findAll();
        assertThat(actualEvents).hasSize(0);
        List<Event> expectedEvents = List.of(expectedEvent1,expectedEvent2);
        assertThat(actualEvents).usingRecursiveComparison()
                .isNotEqualTo(expectedEvents);
    }
    @Test
    public void deleteAllEvent_DeletesNothing_WhenEventsNotExists() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event existingEvent = new Event(
                "123",
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
        underTest.save(existingEvent);

        Event notExistingEvent1 = new Event(
                "456",
                "not-existing-1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        Event notExistingEvent2 = new Event(
                "789",
                "not-existing-2",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        List<Event> passedEvents = List.of(notExistingEvent1,notExistingEvent2);
        // Act
        underTest.deleteAll(passedEvents);

        // Assert
        List<Event> actualEvents = underTest.findAll();
        assertThat(actualEvents).hasSize(1);

        List<Event> notExpectedEvents = List.of(notExistingEvent1,notExistingEvent2);
        assertThat(actualEvents).usingRecursiveComparison()
                .isNotEqualTo(notExpectedEvents);

        List<Event> expectedEvents = List.of(existingEvent);
        assertThat(actualEvents).usingRecursiveComparison()
                .isEqualTo(expectedEvents);
    }

    @Test
    public void deleteAllEvent_DeletesOnlyEventsThatExists() {
        // Arrange
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event existingEvent = new Event(
                "123",
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
        underTest.save(existingEvent);

        Event existingEventToDelete = new Event(
                "456",
                "not-existing-1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(existingEventToDelete);

        Event notExistingEventToDelete = new Event(
                "789",
                "not-existing-2",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);

        List<Event> passedEvents = List.of(existingEventToDelete,notExistingEventToDelete);
        // Act
        underTest.deleteAll(passedEvents);

        // Assert
        List<Event> actualEvents = underTest.findAll();
        assertThat(actualEvents).hasSize(1);

        List<Event> notExpectedEvents = List.of(existingEventToDelete,notExistingEventToDelete);
        assertThat(actualEvents).usingRecursiveComparison()
                .isNotEqualTo(notExpectedEvents);

        List<Event> expectedEvents = List.of(existingEvent);
        assertThat(actualEvents).usingRecursiveComparison()
                .isEqualTo(expectedEvents);
    }

    @Test
    public void deleteById_DeletesNothing_WhenEventsNotExists() {
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event existingEvent1 = new Event(
                "123",
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
        underTest.save(existingEvent1);

        Event existingEvent2 = new Event(
                "456",
                "not-existing-1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(existingEvent2);
        long countBeforeExecuting = underTest.findAll().size();

        // Act
        underTest.deleteById("223");

        // Assert
        long countAfterExecuting = underTest.findAll().size();
        assertThat(countAfterExecuting).isEqualTo(countBeforeExecuting);
    }

    @Test
    public void deleteById_DeletesEvent_WhenEventExists() {
        Guild guild = new Guild("1", "guild");
        saveGuildToDatabase(guild);

        List<Event> events = new ArrayList<>();
        EmbedType embedType = new EmbedType(
                1L,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                events
        );
        saveEmbedTypeToDatabase(embedType);

        Event existingEvent1 = new Event(
                "123",
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
        underTest.save(existingEvent1);

        Event existingEvent2 = new Event(
                "456",
                "not-existing-1",
                "description",
                "12345",
                Instant.now(),
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
        underTest.save(existingEvent2);
        long countBeforeExecuting = underTest.findAll().size();
        long expectedCount = countBeforeExecuting - 1;

        // Act
        underTest.deleteById("123");

        // Assert
        long actualCount = underTest.findAll().size();
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    // Helper methods
    public void saveGuildToDatabase(Guild guild) {
        guildRepository.save(guild);
        Optional<Guild> actual = guildRepository.findById(guild.getId());

        assertThat(actual)
                .withFailMessage("Unable to fetch Guild object from guildRepository")
                .isPresent()
                .hasValueSatisfying(g -> {
                    assertThat(g.getId()).isEqualTo(guild.getId());
                    assertThat(g.getName()).isEqualTo(guild.getName());
                    assertThat(g.getEvents()).isEqualTo(guild.getEvents());
                });
    }

    public void saveEmbedTypeToDatabase(EmbedType embedType) {
        embedTypeRepository.save(embedType);
        Optional<EmbedType> actual = embedTypeRepository.findById(embedType.getId());

        assertThat(actual).hasValueSatisfying(e -> {
            assertThat(e.getId()).isEqualTo(embedType.getId());
            assertThat(e.getName()).isEqualTo(embedType.getName());
            assertThat(e.getStructure()).isEqualTo(embedType.getStructure());
            assertThat(e.getEvents()).usingRecursiveComparison().isEqualTo(embedType.getEvents());
        });
    }

    public Event saveEventToDatabase(Event event, Guild guild, EmbedType embedType) {
        if (!guildRepository.existsById(guild.getId())) {
            saveGuildToDatabase(guild);
        }
        if (!embedTypeRepository.existsById(embedType.getId())) {
            saveEmbedTypeToDatabase(embedType);
        }
        underTest.save(event);
        Optional<Event> actual = underTest.findById(event.getEventId());
        assertThat(actual)
                .isPresent()
                .hasValueSatisfying(e -> {
                    assertThat(e.getEventId()).isEqualTo(event.getEventId());
                    assertThat(e.getName()).isEqualTo(event.getName());
                    assertThat(e.getDescription()).isEqualTo(event.getDescription());
                    assertThat(e.getAuthor()).isEqualTo(event.getAuthor());
                    assertThat(e.getDateTime()).isEqualTo(event.getDateTime());
                    assertThat(e.getDestinationChannelId()).isEqualTo(event.getDestinationChannelId());
                    assertThat(e.getMemberSize()).isEqualTo(event.getMemberSize());
                    assertThat(e.getStatus()).isEqualTo(event.getStatus());
                    assertThat(e.isReminderSent()).isEqualTo(event.isReminderSent());
                    assertThat(e.getInstances()).isEqualTo(event.getInstances());
                    assertThat(e.getParticipants()).usingRecursiveComparison().isEqualTo(event.getParticipants());
                });

        return actual.get();
    }

    private Event createRepositoryEvent(
            String eventId,
            String name,
            Instant dateTime,
            Guild guild,
            EmbedType embedType
    ) {
        return new Event(
                eventId,
                name,
                "description",
                "12345",
                dateTime,
                "123456",
                null,
                "15",
                new ArrayList<>(),
                guild,
                embedType);
    }
}
