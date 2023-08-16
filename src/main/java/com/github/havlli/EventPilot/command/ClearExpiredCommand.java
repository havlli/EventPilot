package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@Component
public class ClearExpiredCommand implements SlashCommand {

    private static final String EVENT_NAME = "clear-expired";
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

    private final SelectMenuComponent expiredSelectMenu;

    public ClearExpiredCommand(SelectMenuComponent expiredSelectMenu) {
        this.expiredSelectMenu = expiredSelectMenu;
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

        if (!interactionEvent.getCommandName().equals(EVENT_NAME)) {
            return Mono.empty();
        }

        SimplePermissionChecker permissionChecker =
                new SimplePermissionChecker(interactionEvent, Permission.MANAGE_CHANNELS);

        return interactionEvent.deferReply()
                .withEphemeral(true)
                .then(permissionChecker.followupWith(deferredResponse(interactionEvent)));
    }

    private Mono<Message> deferredResponse(ChatInputInteractionEvent event) {
        return event.getInteraction()
                .getChannel()
                .flatMapMany(messageChannel -> messageChannel.getMessagesAfter(Snowflake.of(0))
                        .filter(filterBotMessages())
                        .filter(filterExpired())
                        .flatMap(message -> message.delete()
                                .thenReturn(message))
                )
                .collectList()
                .flatMap(messages -> {
                    int count = messages.size();
                    String response;
                    if (count < 1) {
                        response = "No expired events found in this channel.";
                    } else {
                        String messagePlural = count == 1 ? "event" : "events";
                        response = String.format("Deleted %d %s in this channel.", count, messagePlural);
                    }

                    return event.createFollowup(response);
                });
    }

    private Predicate<Message> filterBotMessages() {
        return message -> message.getAuthor().map(User::isBot).orElse(false);
    }

    private Predicate<Message> filterExpired() {
        String expiredCustomTag = expiredSelectMenu.getCustomId();
        return message -> message.getComponents().stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .filter(messageComponent -> messageComponent instanceof SelectMenu)
                .map(messageComponent -> (SelectMenu) messageComponent)
                .anyMatch(selectMenu -> selectMenu.getCustomId().equals(expiredCustomTag));
    }
}
