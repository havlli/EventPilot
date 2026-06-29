package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.entity.guild.GuildService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class StartupTask {

    private final GuildService guildService;
    private final GatewayDiscordClient client;

    public StartupTask(
            GuildService guildService,
            GatewayDiscordClient client
    ) {
        this.guildService = guildService;
        this.client = client;
    }

    public Flux<Guild> handleNewGuilds() {
        return client.getGuilds()
                .flatMap(guild -> {
                    String id = guild.getId().asString();
                    String name = guild.getName();
                    guildService.createGuildIfNotExists(id, name);
                    return Mono.just(guild);
                });
    }
}
