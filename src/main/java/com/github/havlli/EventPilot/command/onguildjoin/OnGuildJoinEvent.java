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
        GuildCreateEvent guildCreateEvent = (GuildCreateEvent) event;
        String name = extractGuildName(guildCreateEvent);
        String id = extractGuildId(guildCreateEvent);
        return createGuildIfNotExists(name, id);
    }

    private Mono<Void> createGuildIfNotExists(String name, String id) {
        System.out.println("guild join event triggered");
        guildService.createGuildIfNotExists(id, name);
        return Mono.empty();
    }

    private static String extractGuildId(GuildCreateEvent event) {
        return event.getGuild().getId().asString();
    }

    private static String extractGuildName(GuildCreateEvent event) {
        return event.getGuild().getName();
    }


}
