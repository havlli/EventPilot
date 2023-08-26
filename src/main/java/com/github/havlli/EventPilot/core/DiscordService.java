package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.component.selectmenu.ExpiredSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.http.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DiscordService {

    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    private static final SelectMenuComponent EXPIRED_COMPONENT = new ExpiredSelectMenu();
    private final GatewayDiscordClient client;

    public DiscordService(GatewayDiscordClient client) {
        this.client = client;
    }

    public Mono<Message> deactivateEvent(Event event) {
        Snowflake messageId = Snowflake.of(event.getEventId());
        Snowflake channelId = Snowflake.of(event.getDestinationChannelId());

        return client.getMessageById(channelId, messageId)
                .flatMap(message -> {
                    if (isAlreadyDeactivated(message)) return Mono.empty();
                    else return deactivateEventMessage(message);
                })
                .onErrorResume(ClientException.class, e -> {
                    String errorMessage = "Message {%s} was not found in channel {%s}"
                            .formatted(messageId.asString(), channelId.asString());
                    logger.error(errorMessage, e);
                    return Mono.empty();
                });
    }

    public Flux<Message> deactivateEvents(List<Event> events) {
        return Flux.fromIterable(events)
                .flatMap(this::deactivateEvent);
    }

    private Mono<Message> deactivateEventMessage(Message message) {
        MessageEditSpec editedMessage = MessageEditSpec.builder()
                .addComponent(EXPIRED_COMPONENT.getActionRow())
                .build();

        return message.edit(editedMessage);
    }

    private boolean isAlreadyDeactivated(Message message) {
        return message.getComponents().stream()
                .findFirst()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream()
                        .findFirst())
                .flatMap(messageComponent -> messageComponent.getData()
                        .customId()
                        .toOptional())
                .filter(id -> id.equals(EXPIRED_COMPONENT.getCustomId()))
                .isPresent();
    }
}
