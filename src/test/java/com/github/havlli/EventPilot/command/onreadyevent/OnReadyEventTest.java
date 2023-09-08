package com.github.havlli.EventPilot.command.onreadyevent;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OnReadyEventTest {

    private AutoCloseable autoCloseable;
    private OnReadyEvent underTest;
    @Mock
    private StartupTask startupTaskMock;
    @Mock
    private ScheduledTask scheduledTaskMock;
    @Mock
    private ReadyEvent readyEventMock;


    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new OnReadyEvent(startupTaskMock, scheduledTaskMock);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void handle() {
        // Arrange
        Guild guildMock = mock(Guild.class);
        when(startupTaskMock.handleNewGuilds()).thenReturn(Flux.just(guildMock));
        when(startupTaskMock.subscribeEventInteractions()).thenReturn(Mono.empty());
        when(scheduledTaskMock.getSchedulersFlux()).thenReturn(Flux.empty());

        // Act
        Mono<?> actualMono = underTest.handle(readyEventMock);

        // Assert
        verify(startupTaskMock, times(1)).handleNewGuilds();
        verify(startupTaskMock, times(1)).subscribeEventInteractions();
        verify(scheduledTaskMock, times(1)).getSchedulersFlux();
        StepVerifier.create(actualMono)
                .verifyComplete();
    }

    @Test
    void getName() {
        // Arrange
        String expected = "on-ready";

        // Act
        String actual = underTest.getName();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void setEventType() {
        // Arrange
        Class<? extends Event> expected = ReadyEvent.class;

        // Act
        underTest.setEventType(expected);

        // Assert
        Class<? extends Event> actual = underTest.getEventType();
        assertThat(actual).isEqualTo(expected);
    }
}