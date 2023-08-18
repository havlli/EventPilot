package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MessageCreator {

    public Mono<Message> permissionsNotValid(ChatInputInteractionEvent interactionEvent) {
        return createFollowupEphemeral(
                interactionEvent,
                "You do not have permission to use this command."
        );
    }

    public Mono<Message> notValidMember(ChatInputInteractionEvent interactionEvent) {
        return createFollowupEphemeral(
                interactionEvent,
                "You are not valid member of this guild!"
        );
    }

    private Mono<Message> createFollowupEphemeral(ChatInputInteractionEvent interactionEvent, String message) {
        return interactionEvent
                .createFollowup(message)
                .withEphemeral(true);
    }
}
