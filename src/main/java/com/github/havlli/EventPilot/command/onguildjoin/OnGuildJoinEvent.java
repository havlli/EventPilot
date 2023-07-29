package com.github.havlli.EventPilot.command.onguildjoin;

import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OnGuildJoinEvent implements SlashCommand {

    private final GuildService guildService;
    private Class<? extends Event> eventType = GuildCreateEvent.class;

    public OnGuildJoinEvent(GuildService guildService) {
        this.guildService = guildService;
    }

    @Override
    public String getName() {
        return "on-guild-join";
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
        GuildCreateEvent onCreateEvent = (GuildCreateEvent) event;

        String guildName = onCreateEvent.getGuild().getName();
        String guildId = onCreateEvent.getGuild().getId().asString();
        guildService.createGuildIfNotExists(guildId, guildName);

        return Mono.empty();
    }
}
