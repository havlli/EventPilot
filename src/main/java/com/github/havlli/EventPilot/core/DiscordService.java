package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.CustomComponentFactory;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
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
import java.util.function.Function;

@Service
public class DiscordService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscordService.class);
    private final CustomComponentFactory componentFactory;
    private final GatewayDiscordClient client;
    private final EventService eventService;
    private final EmbedGenerator embedGenerator;

    public DiscordService(
            GatewayDiscordClient client,
            EventService eventService,
            CustomComponentFactory componentFactory,
            EmbedGenerator embedGenerator
    ) {
        this.client = client;
        this.eventService = eventService;
        this.componentFactory = componentFactory;
        this.embedGenerator = embedGenerator;
    }

    public Flux<Message> deactivateEvents(List<Event> events) {
        return Flux.fromIterable(events)
                .flatMap(this::deactivateEvent);
    }

    public Mono<Message> updateEventMessage(Event event) {
        return client.getMessageById(Snowflake.of(event.getDestinationChannelId()), Snowflake.of(event.getEventId()))
                .flatMap(message -> message.edit(MessageEditSpec.builder()
                        .addEmbed(embedGenerator.generateEmbed(event))
                        .build()));
    }

    private Mono<Message> deactivateEvent(Event event) {
        Snowflake messageId = Snowflake.of(event.getEventId());
        Snowflake channelId = Snowflake.of(event.getDestinationChannelId());

        return client.getMessageById(channelId, messageId)
                .flatMap(this::handleMessage)
                .onErrorResume(ClientException.class, handleMessageNotFound(messageId, channelId));
    }

    private Mono<Message> handleMessage(Message message) {
        return isDeactivated(message) ? completeSignal() : deactivateMessage(message);
    }

    private Function<ClientException, Mono<? extends Message>> handleMessageNotFound(Snowflake messageId, Snowflake channelId) {
        return e -> {
            LOG.error("Message %s was not found in channel %s".formatted(messageId.asString(), channelId.asString()));
            return completeSignal();
        };
    }

    private boolean isDeactivated(Message message) {
        return message.getComponents().stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().toOptional())
                .anyMatch(id -> id.isPresent() && id.get().equals(getExpiredSelectMenu().getCustomId()));
    }

    private Mono<Message> completeSignal() {
        return Mono.empty();
    }

    private Mono<Message> deactivateMessage(Message message) {
        return message.edit(editMessageWithDeactivatedComponent())
                .doOnSuccess(this::deleteEventFromDatabase);
    }

    private void deleteEventFromDatabase(Message message) {
        eventService.deleteEventById(message.getId().asString());
    }

    private MessageEditSpec editMessageWithDeactivatedComponent() {
        return MessageEditSpec.builder()
                .addComponent(getExpiredSelectMenu().getActionRow())
                .build();
    }

    private SelectMenuComponent getExpiredSelectMenu() {
        return componentFactory.getDefaultSelectMenu(CustomComponentFactory.SelectMenuType.EXPIRED_SELECT_MENU);
    }
}
