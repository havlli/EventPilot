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

    private static final String EVENT_NAME = "create-event";
    private final CreateEventInteraction createEventInteraction;
    private final SimplePermissionChecker permissionChecker;
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

    public CreateEventCommand(CreateEventInteraction createEventInteraction, SimplePermissionChecker permissionChecker) {
        this.createEventInteraction = createEventInteraction;
        this.permissionChecker = permissionChecker;
    }

    @Override
    public String getName() {
        return EVENT_NAME;
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

        return interactionEvent.deferReply()
                .withEphemeral(true)
                .then(permissionChecker.followupWith(
                        interactionEvent,
                        Permission.MANAGE_CHANNELS,
                        followupMessage(interactionEvent)
                ));
    }

    private Mono<Message> followupMessage(ChatInputInteractionEvent event) {
        String prompt = "Initiated process of creating event in your DMs, please continue there!";
        return event.createFollowup(prompt)
                .withEphemeral(true)
                .flatMap(ignored -> createEventInteraction.start(event));
    }
}
