package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Component
public class DeleteEventCommand implements SlashCommand {

    private static final String EVENT_NAME = "delete-event";
    private static final String OPTION_MESSAGE_ID = "message-id";
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;
    private final SimplePermissionValidator permissionChecker;
    private final UserSessionValidator userSessionValidator;

    public DeleteEventCommand(SimplePermissionValidator permissionChecker, UserSessionValidator userSessionValidator) {
        this.permissionChecker = permissionChecker;
        this.userSessionValidator = userSessionValidator;
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

    private boolean isValidEvent(ChatInputInteractionEvent interactionEvent) {
        return interactionEvent.getCommandName().equals(EVENT_NAME);
    }

    private Mono<Object> terminateInteraction() {
        return Mono.empty();
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent interactionEvent) {
        return interactionEvent.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionChecker.followupWith(validateSession(event), event, Permission.MANAGE_CHANNELS);
    }

    private Mono<Message> validateSession(ChatInputInteractionEvent event) {
        return userSessionValidator.validateThenWrap(deleteEventInteraction(event), event);
    }

    public Mono<Message> deleteEventInteraction(ChatInputInteractionEvent event) {
        return event.getInteraction().getChannel()
                .flatMap(messageChannel -> {
                    Snowflake targetMessageId = getTargetMessageId(event);
                    return messageChannel.getMessageById(targetMessageId)
                            .flatMap(handleMessageFound(event))
                            .onErrorResume(handleMessageNotFound(event));
                });
    }

    private Function<Throwable, Mono<? extends Message>> handleMessageNotFound(ChatInputInteractionEvent event) {
        return e -> sendMessage(event, "Event not found!");
    }

    private Function<Message, Mono<? extends Message>> handleMessageFound(ChatInputInteractionEvent event) {
        return message -> {
            Optional<User> author = message.getAuthor();
            if (author.isPresent() && author.get().getId().equals(event.getClient().getSelfId())) {
                return deleteMessage(event, message);
            } else {
                return sendMessage(event, "Event not found, already deleted or not posted by this bot!");
            }
        };
    }

    private static Snowflake getTargetMessageId(ChatInputInteractionEvent event) {
        return event.getOption(OPTION_MESSAGE_ID)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> Snowflake.of(value.asString()))
                .orElse(Snowflake.of(0));
    }

    public Mono<Message> sendMessage(ChatInputInteractionEvent event, String content) {
        return event.createFollowup(content)
                .withEphemeral(true)
                .flatMap(Mono::just);
    }

    public Mono<Message> deleteMessage(ChatInputInteractionEvent event, Message message) {
        return message.delete()
                .then(sendMessage(event, "Event deleted!"));
    }
}
