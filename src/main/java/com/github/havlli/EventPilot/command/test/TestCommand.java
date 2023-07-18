package com.github.havlli.EventPilot.command.test;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TestCommand implements SlashCommand {
    @Override
    public String getName() {
        return "test";
    }

    @Override
    public Class<? extends Event> getEventType() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Mono<?> handle(Event event) {
        ChatInputInteractionEvent interactionEvent = (ChatInputInteractionEvent) event;
        if (!interactionEvent.getCommandName().equals(getName())) {
            return Mono.empty();
        }

        return interactionEvent.reply()
                .withEphemeral(true)
                .withContent(interactionEvent.getInteraction().getUser().getUsername());
    }
}
