package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class SimplePermissionChecker {

    private final ChatInputInteractionEvent interactionEvent;

    private final Permission permission;

    public SimplePermissionChecker(ChatInputInteractionEvent interactionEvent, Permission permission) {
        this.interactionEvent = interactionEvent;
        this.permission = permission;
    }

    public Mono<Message> followup(Mono<Message> followupMono) {
        Optional<Member> optionalMember = interactionEvent.getInteraction().getMember();
        if (optionalMember.isEmpty()) {
            return interactionEvent.createFollowup("You are not valid Member to use this command")
                    .withEphemeral(true);
        }

        return optionalMember.get()
                .getBasePermissions()
                .flatMap(permissions -> {
                    if (!permissions.contains(permission)) {
                        return interactionEvent.createFollowup("You do not have permission to use this command.")
                                .withEphemeral(true);
                    }

                    return followupMono;
                });
    }
}
