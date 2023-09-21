package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Component
public class SimplePermissionChecker {

    private final MessageCreator messageCreator;

    public SimplePermissionChecker(MessageCreator messageCreator) {
        this.messageCreator = messageCreator;
    }

    public Mono<Message> followupWith(ChatInputInteractionEvent interactionEvent, Permission permission, Mono<Message> followupMono) {
        Optional<Member> optionalMember = interactionEvent.getInteraction().getMember();
        if (optionalMember.isEmpty()) {
            return sendNotValidMemberMessage(interactionEvent);
        }

        return optionalMember.orElseThrow()
                .getBasePermissions()
                .flatMap(validatePermissions(interactionEvent, permission, followupMono));
    }

    private Function<PermissionSet, Mono<? extends Message>> validatePermissions(ChatInputInteractionEvent interactionEvent, Permission permission, Mono<Message> followupMono) {
        return permissions -> {
            if (permissions.contains(permission)) {
                return followupMono;
            } else {
                return sendNotValidPermissionsMessage(interactionEvent);
            }
        };
    }

    private Mono<Message> sendNotValidPermissionsMessage(ChatInputInteractionEvent interactionEvent) {
        return messageCreator.permissionsNotValid(interactionEvent);
    }

    private Mono<Message> sendNotValidMemberMessage(ChatInputInteractionEvent interactionEvent) {
        return messageCreator.notValidMember(interactionEvent);
    }
}
