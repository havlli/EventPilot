package com.github.havlli.EventPilot.core;

import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
}
