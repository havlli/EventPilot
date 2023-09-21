package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.component.selectmenu.ExpiredSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.http.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Service
public class DiscordService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscordService.class);
    private static final SelectMenuComponent EXPIRED_COMPONENT = new ExpiredSelectMenu();
    private final GatewayDiscordClient client;

    public DiscordService(GatewayDiscordClient client) {
        this.client = client;
    }

    public Flux<Message> deactivateEvents(List<Event> events) {
        return Flux.fromIterable(events)
                .flatMap(this::deactivateEvent);
    }

    private Mono<Message> deactivateEvent(Event event) {
        Snowflake messageId = Snowflake.of(event.getEventId());
        Snowflake channelId = Snowflake.of(event.getDestinationChannelId());

        return client.getMessageById(channelId, messageId)
                .flatMap(handleMessage())
                .onErrorResume(ClientException.class, handleMessageNotFound(messageId, channelId));
    }

    private Function<Message, Mono<? extends Message>> handleMessage() {
        return message -> {
            if (isAlreadyDeactivated(message))
                return completeSignal();
            else
                return deactivateEventMessage(message);
        };
    }

    private Function<ClientException, Mono<? extends Message>> handleMessageNotFound(Snowflake messageId, Snowflake channelId) {
        return e -> {
            LOG.error("Message {} was not found in channel {}", messageId.asString(), channelId.asString(), e);
            return completeSignal();
        };
    }

    private boolean isAlreadyDeactivated(Message message) {
        return message.getComponents().stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().toOptional())
                .anyMatch(id -> id.isPresent() && id.get().equals(EXPIRED_COMPONENT.getCustomId()));
    }

    private Mono<Message> completeSignal() {
        return Mono.empty();
    }

    private Mono<Message> deactivateEventMessage(Message message) {
        return message.edit(getMessageWithDeactivatedComponents());
    }

    private MessageEditSpec getMessageWithDeactivatedComponents() {
        ActionRow expiredComponentRow = EXPIRED_COMPONENT.getActionRow();
        return MessageEditSpec.builder()
                .addComponent(expiredComponentRow)
                .build();
    }
}
