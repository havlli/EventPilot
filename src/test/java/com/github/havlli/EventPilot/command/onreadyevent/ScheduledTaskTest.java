package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.test.StepVerifier;

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
    private static final Integer intervalSeconds = 5;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new ScheduledTask(eventServiceMock, discordServiceMock, intervalSeconds);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void getFlux_InvokesServicesTwiceIn15Seconds_WhenIntervalIs5Seconds() {
        // Arrange
        List<Event> expiredEvents = new ArrayList<>();
        when(eventServiceMock.getExpiredEvents()).thenReturn(expiredEvents);
        doNothing().when(discordServiceMock).deactivateEvents(expiredEvents);

        // Assert
        StepVerifier.create(underTest.getFlux())
                .expectSubscription()
                .thenAwait(Duration.ofSeconds(15))
                .thenCancel()
                .verify();

        verify(eventServiceMock, times(2)).getExpiredEvents();
        verify(discordServiceMock, times(2)).deactivateEvents(expiredEvents);
    }

}