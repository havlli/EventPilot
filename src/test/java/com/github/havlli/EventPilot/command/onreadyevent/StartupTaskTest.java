package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.entity.guild.GuildService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class StartupTaskTest {

    private AutoCloseable autoCloseable;
    private StartupTask underTest;
    @Mock
    private GuildService guildServiceMock;
    @Mock
    private GatewayDiscordClient clientMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new StartupTask(guildServiceMock, clientMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
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
