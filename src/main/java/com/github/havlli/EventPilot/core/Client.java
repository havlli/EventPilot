package com.github.havlli.EventPilot.core;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Client {
    private final String token;

    public Client(@Value("${discord.token}") String token) {
        this.token = token;
    }

    @Bean
    public GatewayDiscordClient createDiscordClient() {
        return DiscordClient.create(token)
                .gateway()
                .setInitialPresence(__ -> ClientPresence.online(ClientActivity.listening("to /commands")))
                .login()
                .block();
    }

    @Bean
    public RestClient restClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }
}
