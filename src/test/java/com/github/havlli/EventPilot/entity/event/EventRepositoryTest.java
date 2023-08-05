package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.AbstractDatabaseContainer;
import com.github.havlli.EventPilot.TimeTester;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationContext;

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
class EventRepositoryTest extends AbstractDatabaseContainer {

    @Autowired
    private GuildRepository guildRepository;
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
    void setUp() {
        System.out.printf("Number of beans initialized { %s }%n", applicationContext.getBeanDefinitionCount());
        timeTester = new TimeTester(entityManager.getEntityManager(), postgresSQLContainer);
    }

    @Test
    void testJPAQueryCurrentTimestamp() throws SQLException {
        // Arrange
        Instant expected = Instant.now();
        // tolerance for assertion to count with execution time
        int toleranceMilliseconds = 45;

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
    void findAllWithDatetimeBeforeCurrentTime_WillReturnListOfExpiredEvents_WhenOffset5Minutes() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(5, ChronoUnit.MINUTES);
        Instant expiredEvent1DateTime = instantNow.minus(5, ChronoUnit.MINUTES);
        Instant expiredEvent2DateTime = instantNow.minus(5, ChronoUnit.MINUTES);

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
                guild
        );
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild);

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
                guild
        );
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild);

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
                guild
        );
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild);

        // Act
        List<Event> actual = underTest.findAllWithDatetimeBeforeCurrentTime();

        String collectActual = actual.stream()
                .map(e -> String.format("\t{ %s - %s }", e.getName(), e.getDateTime()))
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
    void findAllWithDatetimeBeforeCurrentTime_WillReturnListOfExpiredEvents_WhenOffset1Minute() {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(1, ChronoUnit.MINUTES);
        Instant expiredEvent1DateTime = instantNow.minus(1, ChronoUnit.MINUTES);
        Instant expiredEvent2DateTime = instantNow.minus(1, ChronoUnit.MINUTES);

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
                guild
        );
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild);

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
                guild
        );
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild);

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
                guild
        );
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild);

        // Act
        List<Event> actual = underTest.findAllWithDatetimeBeforeCurrentTime();

        String collectActual = actual.stream()
                .map(e -> String.format("\t{ %s - %s }", e.getName(), e.getDateTime()))
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
    void findAllWithDatetimeBeforeCurrentTime_WillReturnListOfExpiredEvents_WhenOffset1Second() throws SQLException {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Instant jdbcTimeAfterInstantNow = timeTester.getCurrentTimestampUsingJdbc();
        Long differenceBetweenSystemAndJdbc = timeTester.calculateTimeDifference.apply(instantNow, jdbcTimeAfterInstantNow);
        System.out.printf("Difference between System Instant.now() and Jdbc CURRENT_TIMESTAMP%n after Instant.Now() invoked - %dms%n", differenceBetweenSystemAndJdbc);

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(1, ChronoUnit.SECONDS);
        Instant expiredEvent1DateTime = instantNow.minus(1, ChronoUnit.SECONDS);
        Instant expiredEvent2DateTime = instantNow.minus(1, ChronoUnit.SECONDS);

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
                guild
        );
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild);

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
                guild
        );
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild);

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
                guild
        );
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild);

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
    void findAllWithDatetimeBeforeCurrentTime_WillReturnListOfExpiredEvents_WhenOffset10Millis() throws SQLException {
        // Arrange
        Instant instantNow = timeTester.getInstantNowFromSystem();
        Instant jdbcTimeAfterInstantNow = timeTester.getCurrentTimestampUsingJdbc();
        Long differenceBetweenSystemAndJdbc = timeTester.calculateTimeDifference.apply(instantNow, jdbcTimeAfterInstantNow);
        System.out.printf("Difference between System Instant.now() and Jdbc CURRENT_TIMESTAMP%n after Instant.Now() invoked - %dms%n", differenceBetweenSystemAndJdbc);

        Guild guild = new Guild("1", "guild");

        Instant validDateTime = instantNow.plus(50, ChronoUnit.MILLIS);
        Instant expiredEvent1DateTime = instantNow.minus(50, ChronoUnit.MILLIS);
        Instant expiredEvent2DateTime = instantNow.minus(50, ChronoUnit.MILLIS);

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
                guild
        );
        Event fetchedValidEvent = saveEventToDatabase(validEvent, guild);

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
                guild
        );
        Event fetchedExpiredEvent1 = saveEventToDatabase(expiredEvent1, guild);

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
                guild
        );
        Event fetchedExpiredEvent2 = saveEventToDatabase(expiredEvent2, guild);

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


    // Helper methods
    Guild saveGuildToDatabase(Guild guild) {
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

        return actual.get();
    }

    Event saveEventToDatabase(Event event, Guild guild) {
        if (!guildRepository.existsById(guild.getId())) {
            saveGuildToDatabase(guild);
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