package com.github.havlli.EventPilot.core;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("discordClient")
public class Client {

    private final GatewayDiscordClient gateway;

    public Client(DiscordClient discordClient) {
        this.gateway = discordClient.login().block();
    }

    @PostConstruct
    public void init() {
        // TODO: Register and subscribe events
        gateway.onDisconnect().block();
    }

    @Bean
    public GatewayDiscordClient getGateway() {
        return this.gateway;
    }
}
