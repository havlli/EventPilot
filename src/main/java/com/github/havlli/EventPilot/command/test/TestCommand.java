package com.github.havlli.EventPilot.command.test;

import com.github.havlli.EventPilot.command.SlashCommand;
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
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply()
                .withEphemeral(true)
                .withContent(event.getInteraction().getUser().getUsername());
    }
}
