package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
public class MessageCreator {

    private final MessageSource messageSource;

    public MessageCreator(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public Mono<Message> permissionsNotValid(ChatInputInteractionEvent interactionEvent) {
        String message = messageSource.getMessage("permissions.not-valid", null, Locale.ENGLISH);
        return createFollowupEphemeral(interactionEvent, message);
    }

    public Mono<Message> notValidMember(ChatInputInteractionEvent interactionEvent) {
        String message = messageSource.getMessage("permissions.not-valid-member", null, Locale.ENGLISH);
        return createFollowupEphemeral(interactionEvent, message);
    }

    public Mono<Message> sessionAlreadyActive(ChatInputInteractionEvent interactionEvent) {
        String message = messageSource.getMessage("sessions.already-active-session", null, Locale.ENGLISH);
        return createFollowupEphemeral(interactionEvent, message);
    }

    private Mono<Message> createFollowupEphemeral(ChatInputInteractionEvent interactionEvent, String message) {
        return interactionEvent.createFollowup(message)
                .withEphemeral(true);
    }
}
