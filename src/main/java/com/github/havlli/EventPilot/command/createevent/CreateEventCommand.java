package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CreateEventCommand implements SlashCommand {
    @Override
    public String getName() {
        return "create-event";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply()
                .withEphemeral(true)
                .withContent("Create Event!");
    }
}
