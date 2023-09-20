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
        ChatInputInteractionEvent interactionEvent = (ChatInputInteractionEvent) event;
        if (!isValidEvent(interactionEvent)) {
            return terminateInteraction();
        }

        return deferInteractionWithEphemeralResponse(interactionEvent)
                .then(validatePermissions(interactionEvent));
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent event) {
        return event.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionChecker.followupWith(
                event,
                Permission.MANAGE_CHANNELS,
                followupMessage(event)
        );
    }

    private Mono<Message> followupMessage(ChatInputInteractionEvent event) {
        String prompt = "Initiated process of creating event in your DMs, please continue there!";
        return event.createFollowup(prompt)
                .withEphemeral(true)
                .flatMap(invokeFinalInteraction(event));
    }

    private Function<Message, Mono<Message>> invokeFinalInteraction(ChatInputInteractionEvent event) {
        return ignored -> createEventInteraction.initiateOn(event);
    }

    private boolean isValidEvent(ChatInputInteractionEvent event) {
        return event.getCommandName().equals(this.getName());
    }

    private Mono<Message> terminateInteraction() {
        return Mono.empty();
    }
}
