package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Permission;
import org.springframework.context.MessageSource;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Optional;

public abstract class OrganizerCommand implements SlashCommand {

    private final String commandName;
    private final SimplePermissionValidator permissionChecker;
    private final UserSessionValidator userSessionValidator;
    private final MessageSource messageSource;
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

    protected OrganizerCommand(
            String commandName,
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource
    ) {
        this.commandName = commandName;
        this.permissionChecker = permissionChecker;
        this.userSessionValidator = userSessionValidator;
        this.messageSource = messageSource;
    }

    @Override
    public String getName() {
        return commandName;
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
        if (!interactionEvent.getCommandName().equals(commandName)) {
            return Mono.empty();
        }

        return deferInteractionWithEphemeralResponse(interactionEvent)
                .then(validatePermissions(interactionEvent));
    }

    protected Optional<Snowflake> getGuildId(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuildId();
    }

    protected String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.ENGLISH);
    }

    protected Mono<Message> sendMessage(ChatInputInteractionEvent event, String content) {
        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                .content(content)
                .ephemeral(true)
                .build());
    }

    protected abstract Mono<Message> processInteraction(ChatInputInteractionEvent event);

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent event) {
        return event.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionChecker.followupWith(validateSession(event), event, Permission.MANAGE_CHANNELS);
    }

    private Mono<Message> validateSession(ChatInputInteractionEvent event) {
        return userSessionValidator.validateThenWrap(processInteraction(event), event);
    }
}
