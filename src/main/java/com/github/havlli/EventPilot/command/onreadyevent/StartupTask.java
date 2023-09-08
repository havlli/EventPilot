package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class StartupTask {

    private static final Logger LOG = LoggerFactory.getLogger(StartupTask.class);
    private final EventService eventService;
    private final GuildService guildService;
    private final EmbedGenerator embedGenerator;
    private final GatewayDiscordClient client;

    public StartupTask(
            EventService eventService,
            GuildService guildService,
            EmbedGenerator embedGenerator,
            GatewayDiscordClient client
    ) {
        this.eventService = eventService;
        this.guildService = guildService;
        this.embedGenerator = embedGenerator;
        this.client = client;
    }

    public Mono<Void> subscribeEventInteractions() {
        List<Event> events = eventService.getAllEvents();
        events.forEach(embedGenerator::subscribeInteractions);
        LOG.info("%d event interactions subscribed".formatted(events.size()));
        return Mono.empty();
    }

    public Flux<Guild> handleNewGuilds() {
        return client.getGuilds().flatMap(guild -> {
            String id = guild.getId().asString();
            String name = guild.getName();
            guildService.createGuildIfNotExists(id, name);
            return Mono.just(guild);
        });
    }
}
