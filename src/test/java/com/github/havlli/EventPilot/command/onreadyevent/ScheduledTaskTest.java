package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

class ScheduledTaskTest {

    private AutoCloseable autoCloseable;
    private ScheduledTask underTest;
    @Mock
    private EventService eventServiceMock;
    @Mock
    private DiscordService discordServiceMock;
    private VirtualTimeScheduler virtualTimeScheduler;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        virtualTimeScheduler = VirtualTimeScheduler.create();
        underTest = new ScheduledTask(
                eventServiceMock,
                discordServiceMock,
                Duration.ofSeconds(5),
                Duration.ofMinutes(60),
                virtualTimeScheduler,
                Schedulers.immediate()
        );
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void getFlux_InvokesExpiryAndReminderServicesTwiceIn13Seconds_WhenIntervalIs5Seconds() {
        // Arrange
        List<Event> expiredEvents = new ArrayList<>();
        List<Event> reminderEvents = new ArrayList<>();
        stubScheduledCycle(expiredEvents, reminderEvents);

        // Assert
        StepVerifier.withVirtualTime(
                        underTest::getSchedulersFlux,
                        () -> virtualTimeScheduler,
                        Long.MAX_VALUE
                )
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(13))
                .thenCancel()
                .verify();

        verify(eventServiceMock, times(2)).getExpiredEvents();
        verify(discordServiceMock, times(2)).deactivateEvents(expiredEvents);
        verify(eventServiceMock, times(2)).getReminderCandidates(Duration.ofMinutes(60));
        verify(discordServiceMock, times(2)).sendEventReminders(reminderEvents);
    }

    @Test
    public void getFlux_SwallowsTransientCycleErrorAndContinuesOnNextTick() {
        // Arrange
        List<Event> expiredEvents = new ArrayList<>();
        List<Event> reminderEvents = new ArrayList<>();
        when(eventServiceMock.getExpiredEvents())
                .thenThrow(new RuntimeException("database unavailable"))
                .thenReturn(expiredEvents);
        when(discordServiceMock.deactivateEvents(expiredEvents)).thenReturn(Flux.empty());
        when(eventServiceMock.getReminderCandidates(Duration.ofMinutes(60))).thenReturn(reminderEvents);
        when(discordServiceMock.sendEventReminders(reminderEvents)).thenReturn(Flux.empty());

        // Assert
        StepVerifier.withVirtualTime(
                        underTest::getSchedulersFlux,
                        () -> virtualTimeScheduler,
                        Long.MAX_VALUE
                )
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(11))
                .thenCancel()
                .verify();

        verify(eventServiceMock, times(2)).getExpiredEvents();
        verify(discordServiceMock, times(1)).deactivateEvents(expiredEvents);
        verify(eventServiceMock, times(1)).getReminderCandidates(Duration.ofMinutes(60));
        verify(discordServiceMock, times(1)).sendEventReminders(reminderEvents);
    }

    @Test
    public void start_IsIdempotentAndStopDisposesSchedulerSubscription() {
        // Arrange
        List<Event> expiredEvents = new ArrayList<>();
        List<Event> reminderEvents = new ArrayList<>();
        stubScheduledCycle(expiredEvents, reminderEvents);

        // Act
        Mono<Void> firstStart = underTest.start();
        Mono<Void> secondStart = underTest.start();

        // Assert
        StepVerifier.create(firstStart)
                .verifyComplete();
        StepVerifier.create(secondStart)
                .verifyComplete();

        virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(5));
        verify(eventServiceMock, times(1)).getExpiredEvents();

        underTest.stop();
        virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(15));
        verify(eventServiceMock, times(1)).getExpiredEvents();

        StepVerifier.create(underTest.start())
                .verifyComplete();
        virtualTimeScheduler.advanceTimeBy(Duration.ofSeconds(5));
        verify(eventServiceMock, times(2)).getExpiredEvents();

        underTest.stop();
    }

    private void stubScheduledCycle(List<Event> expiredEvents, List<Event> reminderEvents) {
        when(eventServiceMock.getExpiredEvents()).thenReturn(expiredEvents);
        when(discordServiceMock.deactivateEvents(expiredEvents)).thenReturn(Flux.empty());
        when(eventServiceMock.getReminderCandidates(Duration.ofMinutes(60))).thenReturn(reminderEvents);
        when(discordServiceMock.sendEventReminders(reminderEvents)).thenReturn(Flux.empty());
    }
}
