package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MessageCreator {

    public Mono<Message> permissionsNotValid(ChatInputInteractionEvent interactionEvent) {
        String message = "You do not have permission to use this command.";
        return createFollowupEphemeral(interactionEvent, message);
    }

    public Mono<Message> notValidMember(ChatInputInteractionEvent interactionEvent) {
        String message = "You are not valid member of this guild!";
        return createFollowupEphemeral(interactionEvent, message);
    }

    public Mono<Message> sessionAlreadyActive(ChatInputInteractionEvent interactionEvent) {
        String message = "You have already one active interaction, finish previous interaction to continue!";
        return createFollowupEphemeral(interactionEvent, message);
    }

    private Mono<Message> createFollowupEphemeral(ChatInputInteractionEvent interactionEvent, String message) {
        return interactionEvent.createFollowup(message)
                .withEphemeral(true);
    }
}
