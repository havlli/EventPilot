package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import com.github.havlli.EventPilot.TimeTester;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeRepository;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EventRepositoryTest extends TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(EventRepositoryTest.class);
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
        // tolerance for assertion to count with execution time
        int toleranceMilliseconds = 100;

        Instant expected = Instant.now();

        // Act
        Instant actualJdbcQuery = timeTester.getCurrentTimestampUsingJdbc();
        Instant actualNativeQuery = timeTester.getCurrentTimestampUsingPersistence();
        Instant actualJPQLQuery = testRepository.selectCurrentTimestamp();

        System.out.printf("expected = %s%nactualNative = %s%nactualJPQL = %s%nactualJdbc = %s%n",
                expected, actualNativeQuery, actualJPQLQuery, actualJdbcQuery);
        // Assert
        assertThat(actualNativeQuery).isCloseTo(expected, Assertions.within(toleranceMilliseconds, ChronoUnit.MILLIS));
        assertThat(actualJPQLQuery).isCloseTo(expected, Assertions.within(toleranceMilliseconds, ChronoUnit.MILLIS));
        assertThat(actualJdbcQuery).isCloseTo(expected, Assertions.within(toleranceMilliseconds, ChronoUnit.MILLIS));
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

        String collectActual = actual.stream()
                .map(e -> String.format("\t{ %s - %s }", e.getName(), e.getDateTime()))
                .collect(Collectors.joining(", \n"));
        String actualPrint = String.format("""
                        instantNow = { %s }
                        actualList = [%n %s %n]
                        """,
                instantNow,
                collectActual);
        System.out.println(actualPrint);

        assertThat(actual).containsOnly(fetchedExpiredEvent1, fetchedExpiredEvent2);
        assertThat(actual).doesNotContain(fetchedValidEvent);
    }

    @Test
    public void findAllWithDatetimeBeforeCurrentTime_ReturnsListOfExpiredEvents_WhenOffsetOneSecond() throws SQLException {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Instant jdbcTimeAfterInstantNow = timeTester.getCurrentTimestampUsingJdbc();
        Long differenceBetweenSystemAndJdbc = timeTester.calculateTimeDifference.apply(instantNow, jdbcTimeAfterInstantNow);
        System.out.printf("Difference between System Instant.now() and Jdbc CURRENT_TIMESTAMP%n after Instant.Now() invoked - %dms%n", differenceBetweenSystemAndJdbc);

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(1, ChronoUnit.SECONDS);
        Instant expiredEvent1DateTime = instantNow.minus(1, ChronoUnit.SECONDS);
        Instant expiredEvent2DateTime = instantNow.minus(1, ChronoUnit.SECONDS);

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

        Instant jdbcTimeAfterQueryExecuted = timeTester.getCurrentTimestampUsingJdbc();
        differenceBetweenSystemAndJdbc = timeTester.calculateTimeDifference.apply(instantNow, jdbcTimeAfterQueryExecuted);
        System.out.printf("Difference between System Instant.now() and Jdbc CURRENT_TIMESTAMP%n after Query executed %dms%n", differenceBetweenSystemAndJdbc);

        String collectActual = actual.stream()
                .map(e -> String.format("\t{ %s - %s | difference between JDBC and Event dateTime - %dms }",
                                e.getName(),
                                e.getDateTime(),
                                timeTester.calculateTimeDifference.apply(jdbcTimeAfterQueryExecuted, e.getDateTime())
                        )
                )
                .collect(Collectors.joining(", \n"));
        System.out.printf("""
                        instantNow = { %s }
                        actualList = [%n %s %n]
                        """,
                instantNow,
                collectActual
        );
        assertThat(actual).containsOnly(fetchedExpiredEvent1, fetchedExpiredEvent2);
        assertThat(actual).doesNotContain(fetchedValidEvent);
    }

    @Test
    public void findAllWithDatetimeBeforeCurrentTime_ReturnsListOfExpiredEvents_WhenOffset250Millis() throws SQLException {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Instant jdbcTimeAfterInstantNow = timeTester.getCurrentTimestampUsingJdbc();
        Long differenceBetweenSystemAndJdbc = timeTester.calculateTimeDifference.apply(instantNow, jdbcTimeAfterInstantNow);
        System.out.printf("Difference between System Instant.now() and Jdbc CURRENT_TIMESTAMP%n after Instant.Now() invoked - %dms%n", differenceBetweenSystemAndJdbc);

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(250, ChronoUnit.MILLIS);
        Instant expiredEvent1DateTime = instantNow.minus(250, ChronoUnit.MILLIS);
        Instant expiredEvent2DateTime = instantNow.minus(250, ChronoUnit.MILLIS);

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
        Instant jdbcTimeAfterQueryExecuted = timeTester.getCurrentTimestampUsingJdbc();
        differenceBetweenSystemAndJdbc = timeTester.calculateTimeDifference.apply(instantNow, jdbcTimeAfterQueryExecuted);
        System.out.printf("Difference between System Instant.now() and Jdbc CURRENT_TIMESTAMP%n after Query executed %dms%n", differenceBetweenSystemAndJdbc);

        String collectActual = actual.stream()
                .map(e -> String.format("\t{ %s - %s | difference between JDBC and Event dateTime - %dms }",
                                e.getName(),
                                e.getDateTime(),
                                timeTester.calculateTimeDifference.apply(jdbcTimeAfterQueryExecuted, e.getDateTime())
                        )
                )
                .collect(Collectors.joining(", \n"));
        System.out.printf("""
                        instantNow = { %s }
                        actualList = [%n %s %n]
                        """,
                instantNow,
                collectActual
        );
        assertThat(actual).containsOnly(fetchedExpiredEvent1, fetchedExpiredEvent2);
        assertThat(actual).doesNotContain(fetchedValidEvent);
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
                .hasValueSatisfying(e -> assertThat(e).isEqualTo(expected));
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
        assertThat(actualEvents).usingRecursiveComparison()
                .isEqualTo(expectedEvents);
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
        System.out.println(actual.get().getGuild().getEvents());
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
                    assertThat(e.getInstances()).isEqualTo(event.getInstances());
                    assertThat(e.getParticipants()).isEqualTo(event.getParticipants());
                });

        return actual.get();
    }
}