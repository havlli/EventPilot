package com.github.havlli.EventPilot.command;

import discord4j.core.event.domain.Event;
import reactor.core.publisher.Mono;

public interface SlashCommand {
    String getName();
    Class<? extends Event> getEventType();
    Mono<?> handle(Event event);
}
