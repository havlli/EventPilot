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
    public void getFlux_InvokesServicesTwiceIn13Seconds_WhenIntervalIs5Seconds() {
        // Arrange
        List<Event> expiredEvents = new ArrayList<>();
        List<Event> reminderEvents = new ArrayList<>();
        when(eventServiceMock.getExpiredEvents()).thenReturn(expiredEvents);
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
                .thenAwait(Duration.ofSeconds(13))
                .thenCancel()
                .verify();

        verify(eventServiceMock, times(2)).getExpiredEvents();
        verify(discordServiceMock, times(2)).deactivateEvents(expiredEvents);
        verify(eventServiceMock, times(2)).getReminderCandidates(Duration.ofMinutes(60));
        verify(discordServiceMock, times(2)).sendEventReminders(reminderEvents);
    }

}
