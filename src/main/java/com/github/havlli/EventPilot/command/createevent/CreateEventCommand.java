package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

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
        ChatInputInteractionEvent chatEvent = (ChatInputInteractionEvent) event;

        if (!isValidEvent(chatEvent)) {
            return terminateInteraction();
        }

        return deferInteractionWithEphemeralResponse(chatEvent)
                .then(validatePermissions(chatEvent));
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent event) {
        return event.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        Permission requiredPermission = Permission.MANAGE_CHANNELS;

        return permissionChecker.followupWith(event, requiredPermission, createFollowupMessage(event));
    }

    private Mono<Message> createFollowupMessage(ChatInputInteractionEvent event) {
        String prompt = "Initiated process of creating event in your DMs, please continue there!";
        return event.createFollowup(prompt)
                .withEphemeral(true)
                .flatMap(invokeFinalInteraction(event));
    }

    private Function<Message, Mono<Message>> invokeFinalInteraction(ChatInputInteractionEvent event) {
        return __ -> createEventInteraction.initiateOn(event);
    }

    private boolean isValidEvent(ChatInputInteractionEvent event) {
        String commandName = event.getCommandName();
        String thisName = this.getName();
        return commandName.equals(thisName);
    }

    private Mono<Message> terminateInteraction() {
        return Mono.empty();
    }
}
