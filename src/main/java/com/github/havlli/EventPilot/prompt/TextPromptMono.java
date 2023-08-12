package com.github.havlli.EventPilot.prompt;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

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
    }

    public Mono<T> mono() {
        return messageChannel.flatMap(channel -> channel.createMessage(messageCreateSpec))
                .flatMap(promptedMessage -> {
                    messageCollector.collect(promptedMessage);

                    return client.getEventDispatcher().on(eventClass)
                            .filter(eventPredicate)
                            .next()
                            .flatMap(event -> {
                                eventProcessor.accept(event);
                                return Mono.just(event);
                            });
                });
    }

    public static class Builder<T extends Event> {
        private Mono<? extends MessageChannel> messageChannel;
        private MessageCollector messageCollector;
        private MessageCreateSpec messageCreateSpec;
        private final GatewayDiscordClient client;
        private final Class<T> eventClass;
        private Consumer<T> eventProcessor;
        private Predicate<T> eventPredicate;

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

        public TextPromptMono<T> build() {
            return new TextPromptMono<>(this);
        }
    }
}
