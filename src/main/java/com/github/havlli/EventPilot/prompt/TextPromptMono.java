package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.selectmenu.ExpiredSelectMenu;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextPromptMono<T extends Event> {

    private final Mono<? extends MessageChannel> messageChannel;
    private final MessageCollector messageCollector;
    private final MessageCreateSpec messageCreateSpec;
    private final GatewayDiscordClient client;
    private final Class<T> eventClass;
    private final Consumer<T> eventProcessor;
    private final Predicate<T> eventPredicate;
    private final String onErrorMessage;
    private final Class<? extends Throwable> errorClass;

    public TextPromptMono(
            TextPromptMono.Builder<T> builder
    ) {
        this.messageChannel = builder.messageChannel;
        this.messageCollector = builder.messageCollector;
        this.messageCreateSpec = builder.messageCreateSpec;
        this.client = builder.client;
        this.eventClass = builder.eventClass;
        this.eventProcessor = builder.eventProcessor;
        this.eventPredicate = builder.eventPredicate;
        this.onErrorMessage = builder.onErrorMessage;
        this.errorClass = builder.errorClass;
    }

    public Mono<T> mono() {
        return messageChannel.flatMap(channel -> channel.createMessage(messageCreateSpec))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    Mono<T> responseMono = constructResponse();

                    if (onErrorMessage != null) {
                        responseMono = responseMono.onErrorResume(errorClass, e -> messageChannel
                                .flatMap(channel -> channel.createMessage(onErrorMessage))
                                .flatMap(message -> {
                                    messageCollector.collect(message);
                                    return this.mono();
                                }));
                    }

                    return responseMono;
                });
    }

    private Mono<T> constructResponse() {
        if (eventClass.getCanonicalName().equals(SelectMenuInteractionEvent.class.getCanonicalName())) {
            ExpiredSelectMenu expiredMenu = new ExpiredSelectMenu();
            return client.getEventDispatcher().on(eventClass)
                    .filter(eventPredicate)
                    .next()
                    .flatMap(event -> {
                        eventProcessor.accept(event);
                        SelectMenuInteractionEvent interactionEvent = ((SelectMenuInteractionEvent) event);
                        return interactionEvent.deferEdit()
                                .then(interactionEvent.editReply(InteractionReplyEditSpec.builder()
                                        .components(List.of(expiredMenu.getDisabledRow()))
                                        .build()))
                                .then(Mono.just(event));
                    });

        } else if (eventClass.getCanonicalName().equals(ButtonInteractionEvent.class.getCanonicalName())) {
            return client.getEventDispatcher().on(eventClass)
                    .filter(eventPredicate)
                    .next()
                    .flatMap(event -> {
                        eventProcessor.accept(event);
                        ButtonInteractionEvent interactionEvent = ((ButtonInteractionEvent) event);
                        return interactionEvent.deferEdit()
                                .then(interactionEvent.getInteractionResponse().deleteInitialResponse())
                                .then(Mono.just(event));
                    });

        } else if (eventClass.getCanonicalName().equals(MessageCreateEvent.class.getCanonicalName())) {
            return client.getEventDispatcher().on(eventClass)
                    .filter(eventPredicate)
                    .next()
                    .flatMap(event -> {
                        eventProcessor.accept(event);
                        return Mono.just(event);
                    });
        } else {
            throw new IllegalStateException(eventClass + " not supported!");
        }
    }

    public static class Builder<T extends Event> {
        private Mono<? extends MessageChannel> messageChannel;
        private MessageCollector messageCollector;
        private MessageCreateSpec messageCreateSpec;
        private final GatewayDiscordClient client;
        private final Class<T> eventClass;
        private Consumer<T> eventProcessor;
        private Predicate<T> eventPredicate;
        private String onErrorMessage;
        private Class<? extends Throwable> errorClass;

        public Builder(GatewayDiscordClient client, Class<T> eventClass) {
            this.client = client;
            this.eventClass = eventClass;
        }

        public Builder<T> messageChannel(Mono<? extends MessageChannel> messageChannel) {
            this.messageChannel = messageChannel;
            return this;
        }

        public Builder<T> messageCreateSpec(MessageCreateSpec messageCreateSpec) {
            this.messageCreateSpec = messageCreateSpec;
            return this;
        }

        public Builder<T> eventProcessor(Consumer<T> eventProcessor) {
            this.eventProcessor = eventProcessor;
            return this;
        }

        public Builder<T> withMessageCollector(MessageCollector messageCollector) {
            this.messageCollector = messageCollector;
            return this;
        }

        public Builder<T> eventPredicate(Predicate<T> eventPredicate) {
            this.eventPredicate = eventPredicate;
            return this;
        }

        public Builder<T> onErrorRepeat(Class<? extends Throwable> errorClass, String onErrorMessage) {
            this.onErrorMessage = onErrorMessage;
            this.errorClass = errorClass;
            return this;
        }

        public TextPromptMono<T> build() {
            return new TextPromptMono<>(this);
        }
    }
}
