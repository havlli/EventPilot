package com.github.havlli.EventPilot.command.createembedtype;

import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Permission;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

@Component
public class CreateEmbedTypeCommand implements SlashCommand {

    private final static String EVENT_NAME = "create-embed-type";
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;
    private final SimplePermissionValidator permissionValidator;
    private final UserSessionValidator userSessionValidator;
    private final CreateEmbedTypeInteraction createEmbedTypeInteraction;
    private final MessageSource messageSource;

    public CreateEmbedTypeCommand(
            SimplePermissionValidator permissionValidator,
            UserSessionValidator userSessionValidator,
            CreateEmbedTypeInteraction createEmbedTypeInteraction,
            MessageSource messageSource
    ) {
        this.permissionValidator = permissionValidator;
        this.userSessionValidator = userSessionValidator;
        this.createEmbedTypeInteraction = createEmbedTypeInteraction;
        this.messageSource = messageSource;
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
        ChatInputInteractionEvent interactionEvent = ((ChatInputInteractionEvent) event);

        if (!isValidEvent(interactionEvent)) {
            return terminateInteraction();
        }

        return deferInteractionWithEphemeralResponse(interactionEvent)
                .then(validatePermissions(interactionEvent));
    }

    private boolean isValidEvent(ChatInputInteractionEvent event) {
        return event.getCommandName().equals(EVENT_NAME);
    }

    private Mono<Object> terminateInteraction() {
        return Mono.empty();
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent interactionEvent) {
        return interactionEvent.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionValidator.followupWith(validateSessions(event), event, Permission.MANAGE_CHANNELS);
    }

    private Mono<Message> validateSessions(ChatInputInteractionEvent event) {
        return userSessionValidator.validateThenWrap(createFollowupMessage(event), event);
    }

    private Mono<Message> createFollowupMessage(ChatInputInteractionEvent event) {
        String prompt = messageSource.getMessage("interaction.private.initiated", null, Locale.ENGLISH);
        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .content(prompt)
                        .ephemeral(true)
                        .build())
                .then(invokePrivateInteraction(event));
    }

    private Mono<Message> invokePrivateInteraction(ChatInputInteractionEvent event) {
        return createEmbedTypeInteraction.initiateOn(event);
    }
}
