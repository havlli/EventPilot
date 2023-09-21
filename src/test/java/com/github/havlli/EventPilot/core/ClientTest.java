package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.DiscordBotTestConfig;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTest extends DiscordBotTestConfig {

    private Client underTest;

    @BeforeEach
    void setUp() {
        underTest = new Client(getToken());
    }

    @Test
    public void discordClient_ConnectsToTestBot() {
        // Act
        GatewayDiscordClient gatewayDiscordClient = underTest.createDiscordClient();

        // Assert
        assertThat(gatewayDiscordClient.getSelfId()).isEqualTo(Snowflake.of(1142488932903309383L));
    }

    @Test
    public void restClient_ReturnsRestClient() {
        // Arrange
        GatewayDiscordClient gatewayDiscordClient = underTest.createDiscordClient();

        // Act
        RestClient restClient = underTest.restClient(gatewayDiscordClient);

        // Assert
        assertThat(restClient).isNotNull();
    }
}