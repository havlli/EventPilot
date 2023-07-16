package com.github.havlli.EventPilot.core;

import discord4j.core.DiscordClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Value("${discord.token}")
    private String token;

    @Bean
    public DiscordClient discordClient() {
        return DiscordClient.create(token);
    }
}
