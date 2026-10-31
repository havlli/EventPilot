package com.github.havlli.EventPilot.core;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class Client {
    private final DiscordProperties discordProperties;

    public Client(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
    }

    @Bean
    public GatewayDiscordClient createDiscordClient() {
        return DiscordClient.create(discordProperties.token())
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
