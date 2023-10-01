package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class ClearExpiredCommand implements SlashCommand {

    private static final String EVENT_NAME = "clear-expired";
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

    private final SimplePermissionValidator permissionChecker;
    private final SelectMenuComponent expiredSelectMenu;
    private final UserSessionValidator userSessionValidator;

    public ClearExpiredCommand(
            SimplePermissionValidator permissionChecker,
            SelectMenuComponent expiredSelectMenu,
            UserSessionValidator userSessionValidator
    ) {
        this.permissionChecker = permissionChecker;
        this.expiredSelectMenu = expiredSelectMenu;
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
                .then(validatePermissions(interactionEvent))
                .doFinally(__ -> deleteSession(interactionEvent));
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
        return permissionChecker.followupWith(validateSessions(event), event, Permission.MANAGE_CHANNELS);
    }

    private void deleteSession(ChatInputInteractionEvent event) {
        userSessionValidator.terminate(event);
    }

    private Mono<Message> validateSessions(ChatInputInteractionEvent event) {
        return userSessionValidator.validate(followupResponse(event), event);
    }

    private Mono<Message> followupResponse(ChatInputInteractionEvent event) {
        return event.getInteraction()
                .getChannel()
                .flatMapMany(this::filterAllMessagesThenDelete)
                .collectList()
                .flatMap(handleResponse(event));
    }

    private Flux<Message> filterAllMessagesThenDelete(MessageChannel messageChannel) {
        return messageChannel.getMessagesAfter(Snowflake.of(0))
                .filter(filterBotMessages())
                .filter(filterExpired())
                .flatMap(this::deleteMessageThenReturn);
    }

    private Function<List<Message>, Mono<? extends Message>> handleResponse(ChatInputInteractionEvent event) {
        return messages -> {
            String response = formatResponseMessage(messages.size());
            return sendResponse(event, response);
        };
    }

    public Predicate<Message> filterBotMessages() {
        return message -> message.getAuthor().map(User::isBot).orElse(false);
    }

    public Predicate<Message> filterExpired() {
        String expiredCustomTag = expiredSelectMenu.getCustomId();
        return message -> message.getComponents().stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .filter(messageComponent -> messageComponent instanceof SelectMenu)
                .map(messageComponent -> (SelectMenu) messageComponent)
                .anyMatch(selectMenu -> selectMenu.getCustomId().equals(expiredCustomTag));
    }

    private Mono<Message> deleteMessageThenReturn(Message message) {
        return message.delete()
                .thenReturn(message);
    }

    private String formatResponseMessage(int count) {
        if (count == 0) {
            return  "No expired events found in this channel.";
        } else {
            String messagePlural = count == 1 ? "event" : "events";
            return String.format("Deleted %d %s in this channel.", count, messagePlural);
        }
    }

    public Mono<Message> sendResponse(ChatInputInteractionEvent event, String response) {
        return event.createFollowup(response);
    }
}
