package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OnReadyEvent implements SlashCommand {

    private final StartupTask startupTask;
    private final GuildService guildService;

    public OnReadyEvent(StartupTask startupTask, GuildService guildService) {
        this.startupTask = startupTask;
        this.guildService = guildService;
    }

    private Class<? extends Event> eventType = ReadyEvent.class;

    @Override
    public String getName() {
        return "on-ready";
    }

    @Override
    public Class<? extends Event> getEventType() {
        return eventType;
    }

    @Override
    public void setEventType(Class<? extends Event> eventType) {
        this.eventType = eventType;
    }

    @Override
    public Mono<?> handle(Event event) {

        return event.getClient()
                .getGuilds()
                .flatMap(guild -> {

                    String guildId = guild.getId().asString();
                    String guildName = guild.getName();
                    guildService.createGuildIfNotExists(guildId, guildName);

                    return Mono.just(guild);
                })
                .then(startupTask.subscribeEventInteractions());
    }
}
