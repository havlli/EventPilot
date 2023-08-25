package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class StartupTaskTest {

    private AutoCloseable autoCloseable;
    private StartupTask underTest;
    @Mock
    private EventService eventServiceMock;
    @Mock
    private GuildService guildServiceMock;
    @Mock
    private EmbedGenerator embedGeneratorMock;
    @Mock
    private GatewayDiscordClient clientMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new StartupTask(eventServiceMock, guildServiceMock, embedGeneratorMock, clientMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void subscribeEventInteractions_subscribeInteractionRunsForEachEvent_WhenMultipleEventsPassed() {
        // Arrange
        Event eventOneMock = mock(Event.class);
        Event eventTwoMock = mock(Event.class);
        Event eventThreeMock = mock(Event.class);
        Event eventFourMock = mock(Event.class);
        List<Event> events = List.of(eventOneMock, eventTwoMock, eventThreeMock, eventFourMock);
        when(eventServiceMock.getAllEvents()).thenReturn(events);

        // Act
        Mono<Void> actualMono = underTest.subscribeEventInteractions();

        // Assert
        StepVerifier.create(actualMono)
                .expectSubscription()
                .verifyComplete();
        verify(embedGeneratorMock, times(events.size())).subscribeInteractions(any());
    }

    @Test
    void subscribeEventInteractions_subscribeInteractionDoesNotRun_WhenNoEventPassed() {
        // Arrange
        List<Event> events = List.of();
        when(eventServiceMock.getAllEvents()).thenReturn(events);

        // Act
        Mono<Void> actualMono = underTest.subscribeEventInteractions();

        // Assert
        StepVerifier.create(actualMono)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(embedGeneratorMock);
    }

    @Test
    void handleNewGuilds_CallsGuildServiceForEachGuild_WhenOneOrMoreGuildsRetrievedFromClient() {
        // Arrange
        Guild guildOneMock = mock(Guild.class);
        Snowflake guildOneId = Snowflake.of("1");
        String guildOneName = "guildOne";
        when(guildOneMock.getId()).thenReturn(guildOneId);
        when(guildOneMock.getName()).thenReturn(guildOneName);

        Guild guildTwoMock = mock(Guild.class);
        Snowflake guildTwoId = Snowflake.of("2");
        String guildTwoName = "guildTwo";
        when(guildTwoMock.getId()).thenReturn(guildTwoId);
        when(guildTwoMock.getName()).thenReturn(guildTwoName);

        Flux<Guild> guildFlux = Flux.just(guildOneMock, guildTwoMock);
        when(clientMock.getGuilds()).thenReturn(guildFlux);

        // Act
        Flux<Guild> actualFlux = underTest.handleNewGuilds();

        // Assert
        StepVerifier.create(actualFlux)
                .expectSubscription()
                .expectNext(guildOneMock, guildTwoMock)
                .verifyComplete();

        verify(guildServiceMock, times(2)).createGuildIfNotExists(anyString(), anyString());
    }

    @Test
    void handleNewGuilds_NoInteractionsWithGuildService_WhenNoGuildsRetrievedFromClient() {
        // Arrange
        Flux<Guild> guildFlux = Flux.just();
        when(clientMock.getGuilds()).thenReturn(guildFlux);

        // Act
        Flux<Guild> actualFlux = underTest.handleNewGuilds();

        // Assert
        StepVerifier.create(actualFlux)
                .expectSubscription()
                .verifyComplete();

        verifyNoInteractions(guildServiceMock);
    }
}