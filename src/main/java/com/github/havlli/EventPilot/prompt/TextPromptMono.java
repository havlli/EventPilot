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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TextPromptMono<T extends Event> {

    private final static Logger LOG = LoggerFactory.getLogger(TextPromptMono.class);
    private final Mono<? extends MessageChannel> messageChannel;
    private final MessageCollector messageCollector;
    private final MessageCreateSpec messageCreateSpec;
    private final GatewayDiscordClient client;
    private final Class<T> eventClass;
    private final Consumer<T> eventProcessor;
    private final Predicate<T> eventPredicate;
    private final String onErrorMessage;
    private final Class<? extends Throwable> errorClass;
    private final PromptType promptType;

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
        this.promptType = builder.promptType;
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
                                    LOG.error("threw error {}\n{}", errorClass.getCanonicalName(), e.getStackTrace());
                                    messageCollector.collect(message);
                                    return this.mono();
                                }));
                    }

                    return responseMono;
                });
    }

    private Mono<T> constructResponse() {
        return client.getEventDispatcher().on(eventClass)
                .filter(eventPredicate)
                .next()
                .flatMap(interactionResponseMono());
    }

    private Function<T, Mono<T>> interactionResponseMono() {
        return event -> {
            eventProcessor.accept(event);

            switch (promptType) {
                case DEFAULT -> {
                    return defaultInteractionResponse().apply(event);
                }
                case DELETE_ON_RESPONSE -> {
                    return onDeleteInteractionResponse().apply(event);
                }
                case DEFERRABLE_EDIT -> {
                    return deferrableEditInteractionResponse().apply(event);
                }
                case DEFERRABLE_REPLY -> {
                    return deferrableReplyInteractionResponse().apply(event);
                }
            }

            throw new IllegalStateException("%s not supported operation".formatted(eventClass));
        };
    }

    private Function<T, Mono<T>> defaultInteractionResponse() {
        return event -> {
            if (event instanceof SelectMenuInteractionEvent selectMenuEvent) {
                ExpiredSelectMenu expiredMenu = new ExpiredSelectMenu();
                return selectMenuEvent.deferEdit()
                        .then(selectMenuEvent.editReply(InteractionReplyEditSpec.builder()
                                .components(List.of(expiredMenu.getDisabledRow()))
                                .build()))
                        .then(Mono.just(event));
            } else if (event instanceof ButtonInteractionEvent buttonEvent) {
                return buttonEvent.deferEdit()
                        .then(buttonEvent.editReply(InteractionReplyEditSpec.builder()
                                .components(List.of())
                                .build()))
                        .then(Mono.just(event));
            } else if (event instanceof MessageCreateEvent) {
                return Mono.just(event);
            }

            throw new IllegalStateException("%s not supported operation for %s".formatted(eventClass, promptType));
        };
    }

    private Function<T, Mono<T>> onDeleteInteractionResponse() {
        return event -> {
            if (event instanceof SelectMenuInteractionEvent selectMenuEvent) {
                return selectMenuEvent.deferEdit()
                        .then(selectMenuEvent.getInteractionResponse().deleteInitialResponse())
                        .then(Mono.just(event));
            } else if (event instanceof ButtonInteractionEvent buttonEvent) {
                return buttonEvent.deferEdit()
                        .then(buttonEvent.getInteractionResponse().deleteInitialResponse())
                        .then(Mono.just(event));
            }

            throw new IllegalStateException("%s not supported operation for %s".formatted(eventClass, promptType));
        };
    }

    private Function<T, Mono<T>> deferrableEditInteractionResponse() {
        return event -> {
            if (event instanceof SelectMenuInteractionEvent selectMenuEvent) {
                return selectMenuEvent.deferEdit()
                        .then(Mono.just(event));
            } else if (event instanceof ButtonInteractionEvent buttonEvent) {
                return buttonEvent.deferEdit()
                        .then(Mono.just(event));
            }

            throw new IllegalStateException("%s not supported operation for %s".formatted(eventClass, promptType));
        };
    }

    private Function<T, Mono<T>> deferrableReplyInteractionResponse() {
        return event -> {
            if (event instanceof SelectMenuInteractionEvent selectMenuEvent) {
                return selectMenuEvent.deferReply()
                        .then(Mono.just(event));
            } else if (event instanceof ButtonInteractionEvent buttonEvent) {
                return buttonEvent.deferReply()
                        .then(Mono.just(event));
            }

            throw new IllegalStateException("%s not supported operation for %s".formatted(eventClass, promptType));
        };
    }

    public enum PromptType {
        DEFAULT,
        DELETE_ON_RESPONSE,
        DEFERRABLE_EDIT,
        DEFERRABLE_REPLY
    }

    public static class Builder<T extends Event> {
        @NotNull
        private Mono<? extends MessageChannel> messageChannel;
        private MessageCollector messageCollector;
        @NotNull
        private MessageCreateSpec messageCreateSpec;
        @NotNull
        private final GatewayDiscordClient client;
        @NotNull
        private final Class<T> eventClass;
        @NotNull
        private Consumer<T> eventProcessor;
        @NotNull
        private Predicate<T> eventPredicate;
        @NotNull
        private PromptType promptType;
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

        public Builder<T> withPromptType(PromptType promptType) {
            this.promptType = promptType;
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
            Objects.requireNonNull(messageChannel, "messageChannel is required");
            Objects.requireNonNull(messageCreateSpec, "messageCreateSpec is required");
            Objects.requireNonNull(eventClass, "eventClass is required");
            Objects.requireNonNull(client, "client is required");
            Objects.requireNonNull(eventProcessor, "eventProcessor is required");
            Objects.requireNonNull(eventPredicate, "eventPredicate is required");
            Objects.requireNonNull(promptType, "eventPredicate is required");
            return new TextPromptMono<>(this);
        }
    }
}
