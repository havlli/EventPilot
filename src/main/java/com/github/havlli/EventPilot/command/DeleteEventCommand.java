package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class DeleteEventCommand implements SlashCommand {

    private static final String EVENT_NAME = "delete-event";
    private static final String OPTION_MESSAGE_ID = "message-id";
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;
    private final SimplePermissionChecker permissionChecker;

    public DeleteEventCommand(SimplePermissionChecker permissionChecker) {
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
        if (interactionEvent.getCommandName().equals(EVENT_NAME)) {
            return interactionEvent.deferReply()
                    .withEphemeral(true)
                    .then(permissionChecker.followupWith(
                            interactionEvent,
                            Permission.MANAGE_CHANNELS,
                            deleteEventInteraction(interactionEvent)
                    ));
        }
        return Mono.empty();
    }

    private Mono<Message> deleteEventInteraction(ChatInputInteractionEvent event) {
        return event.getInteraction().getChannel()
                .flatMap(messageChannel -> {
                    Snowflake messageId = event.getOption(OPTION_MESSAGE_ID)
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(value -> Snowflake.of(value.asString()))
                            .orElse(Snowflake.of(0));
                    return messageChannel.getMessageById(messageId)
                            .flatMap(message -> {
                                Optional<User> author = message.getAuthor();
                                if (author.isPresent() && author.get().getId().equals(event.getClient().getSelfId())) {
                                    return message.delete()
                                            .then(event.createFollowup("Event deleted!")
                                                    .withEphemeral(true));
                                } else {
                                    return event.createFollowup("Event not found, already deleted or not posted by this bot!")
                                            .withEphemeral(true);
                                }
                            })
                            .onErrorResume(e -> event.createFollowup("Event not found!")
                                    .withEphemeral(true));
                });
    }
}
