package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.CustomComponentFactory;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public Flux<Message> sendEventReminders(List<Event> events) {
        return Flux.fromIterable(events)
                .flatMap(this::sendEventReminder);
    }

    public Mono<Message> updateEventMessage(Event event) {
        return client.getMessageById(Snowflake.of(event.getDestinationChannelId()), Snowflake.of(event.getEventId()))
                .flatMap(message -> message.edit(MessageEditSpec.builder()
                        .addEmbed(embedGenerator.generateEmbed(event))
                        .addAllComponents(embedGenerator.generateComponents(event))
                        .build()));
    }

    public Mono<Void> deleteEventMessage(Event event) {
        return client.getMessageById(Snowflake.of(event.getDestinationChannelId()), Snowflake.of(event.getEventId()))
                .flatMap(Message::delete)
                .onErrorResume(error -> {
                    LOG.error("Could not delete message %s in channel %s".formatted(event.getEventId(), event.getDestinationChannelId()));
                    return Mono.empty();
                });
    }

    private Mono<Message> sendEventReminder(Event event) {
        return client.getChannelById(Snowflake.of(event.getDestinationChannelId()))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                        .addEmbed(embedGenerator.generateReminderEmbed(event))
                        .build()))
                .flatMap(message -> markReminderSent(event.getEventId()).thenReturn(message))
                .onErrorResume(error -> {
                    LOG.error("Could not send reminder for event %s".formatted(event.getEventId()), error);
                    return Mono.empty();
                });
    }

    private Mono<Message> deactivateEvent(Event event) {
        Snowflake messageId = Snowflake.of(event.getEventId());
        Snowflake channelId = Snowflake.of(event.getDestinationChannelId());

        return client.getMessageById(channelId, messageId)
                .flatMap(this::handleMessage)
                .onErrorResume(ClientException.class, handleMessageNotFound(messageId, channelId));
    }

    private Mono<Message> handleMessage(Message message) {
        return isDeactivated(message) ? markEventExpired(message).then(completeSignal()) : deactivateMessage(message);
    }

    private Function<ClientException, Mono<? extends Message>> handleMessageNotFound(Snowflake messageId, Snowflake channelId) {
        return e -> {
            LOG.error("Message %s was not found in channel %s".formatted(messageId.asString(), channelId.asString()));
            return markEventExpired(messageId.asString()).then(completeSignal());
        };
    }

    private boolean isDeactivated(Message message) {
        return message.getComponents().stream()
                .filter(LayoutComponent.class::isInstance)
                .map(LayoutComponent.class::cast)
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().toOptional())
                .anyMatch(id -> id.isPresent() && id.get().equals(getExpiredSelectMenu().getCustomId()));
    }

    private Mono<Message> completeSignal() {
        return Mono.empty();
    }

    private Mono<Message> deactivateMessage(Message message) {
        return message.edit(editMessageWithDeactivatedComponent())
                .flatMap(updatedMessage -> markEventExpired(updatedMessage).thenReturn(updatedMessage));
    }

    private Mono<Boolean> markEventExpired(Message message) {
        return markEventExpired(message.getId().asString());
    }

    private Mono<Boolean> markEventExpired(String eventId) {
        return Mono.fromSupplier(() -> eventService.markExpiredIfExists(eventId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Boolean> markReminderSent(String eventId) {
        return Mono.fromSupplier(() -> eventService.markReminderSentIfExists(eventId))
                .subscribeOn(Schedulers.boundedElastic());
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
