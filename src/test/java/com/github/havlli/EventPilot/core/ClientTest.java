package com.github.havlli.EventPilot.core;

import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientTest {

    @Test
    void restClient_ReturnsGatewayRestClient() {
        // Arrange
        Client underTest = new Client(new DiscordProperties(
                "token",
                new DiscordProperties.Commands("commands"),
                new DiscordProperties.Scheduler(60, 60)
        ));
        GatewayDiscordClient gatewayDiscordClient = mock(GatewayDiscordClient.class);
        RestClient expected = mock(RestClient.class);
        when(gatewayDiscordClient.getRestClient()).thenReturn(expected);

        // Act
        RestClient actual = underTest.restClient(gatewayDiscordClient);

        // Assert
        assertThat(actual).isSameAs(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void createDiscordClient_FailsFastWhenTokenMissing(String token) {
        Client underTest = new Client(new DiscordProperties(
                token,
                new DiscordProperties.Commands("commands"),
                new DiscordProperties.Scheduler(60, 60)
        ));

        assertThatThrownBy(underTest::createDiscordClient)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DISCORD_BOT_TOKEN");
    }
}
