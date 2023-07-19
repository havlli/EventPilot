package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CreateEventCommand implements SlashCommand {

    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;
    @Override
    public String getName() {
        return "create-event";
    }

    @Override
    public Class<? extends Event> getEventType() {
        return eventType;
    }

    @Override
    public void setEventType(Class<? extends Event> eventType) {
        this.eventType = eventType;
    }

    @Override
    public Mono<?> handle(Event event) {
        ChatInputInteractionEvent interactionEvent = (ChatInputInteractionEvent) event;
        if (!interactionEvent.getCommandName().equals(getName())) {
            return Mono.empty();
        }

        SimplePermissionChecker permissionChecker = new SimplePermissionChecker(interactionEvent, Permission.MANAGE_CHANNELS);

        return interactionEvent.deferReply()
                .withEphemeral(true)
                .then(permissionChecker.followup(followupMessage(interactionEvent)));
    }

    private Mono<Message> followupMessage(ChatInputInteractionEvent event) {
        return event.createFollowup("Test!").withEphemeral(true);
    }
}
