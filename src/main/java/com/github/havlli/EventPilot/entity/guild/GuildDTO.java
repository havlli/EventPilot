package com.github.havlli.EventPilot.entity.guild;

import com.github.havlli.EventPilot.entity.event.Event;

import java.util.List;

public record GuildDTO(
        String id,
        String name,
        List<String> events
) {
    public static GuildDTO fromGuild(Guild guild) {
        return new GuildDTO(
                guild.getId(),
                guild.getName(),
                guild.getEvents().stream()
                        .map(Event::getEventId)
                        .toList()
        );
    }
}
