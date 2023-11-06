package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ActionRowComponent;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionCallbackSpecDeferEditMono;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class TextPromptBuilder<T extends Event> {

    private final static Logger LOG = LoggerFactory.getLogger(TextPromptBuilder.class);
    private final Mono<? extends MessageChannel> messageChannel;
    private final MessageCollector messageCollector;
    private final MessageCreateSpec messageCreateSpec;
    private final ActionRowComponent actionRowComponent;
    private final GatewayDiscordClient client;
    private final Class<T> eventClass;
    private final Consumer<T> eventProcessor;
    private final Predicate<T> eventPredicate;
    private final String onErrorMessage;
    private final Class<? extends Throwable> errorClass;
    private final PromptType promptType;

    private final static Map<Class<? extends Event>, List<PromptType>> supportedEventOperations = Map.of(
            SelectMenuInteractionEvent.class, List.of(
                    PromptType.DEFAULT,
                    PromptType.DELETE_ON_RESPONSE,
                    PromptType.DEFERRABLE_EDIT,
                    PromptType.DEFERRABLE_REPLY
            ),
            ButtonInteractionEvent.class, List.of(
                    PromptType.DEFAULT,
                    PromptType.DELETE_ON_RESPONSE,
                    PromptType.DEFERRABLE_EDIT,
                    PromptType.DEFERRABLE_REPLY
            ),
            MessageCreateEvent.class, List.of(PromptType.DEFAULT)
    );

    public TextPromptBuilder(
            TextPromptBuilder.Builder<T> builder
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
        this.actionRowComponent = builder.actionRowComponent;
    }

    public Mono<T> createMono() {
        return messageChannel
                .flatMap(this::sendMessageAndCollect)
                .flatMap(this::handleResponse);
    }

    private Mono<Message> sendMessageAndCollect(MessageChannel messageChannel) {
        return messageChannel.createMessage(messageCreateSpec)
                .doOnNext(messageCollector::collect);
    }

    protected Mono<T> handleResponse(Message previousMessage) {
        Mono<T> responseMono = subscribeEventAndCreateResponse();

        if (onErrorMessage != null) {
            responseMono = responseMono.onErrorResume(errorClass, this::handleErrorThenRepeatInteraction);
        }

        return responseMono;
    }

    protected Mono<T> subscribeEventAndCreateResponse() {
        return client.getEventDispatcher().on(eventClass)
                .filter(eventPredicate)
                .next()
                .flatMap(processEventThenCreateResponse());
    }

    protected Mono<T> handleErrorThenRepeatInteraction(Throwable e) {
        return messageChannel
                .flatMap(channel -> channel.createMessage(onErrorMessage))
                .flatMap(message -> {
                    LOG.error("threw error %s\n%s".formatted(errorClass.getCanonicalName(), Arrays.toString(e.getStackTrace())));
                    messageCollector.collect(message);
                    return repeatInteraction();
                });
    }

    private Function<T, Mono<T>> processEventThenCreateResponse() {
        return event -> {
            eventProcessor.accept(event);
            return switch (promptType) {
                case DEFAULT -> defaultResponse().apply(event);
                case DELETE_ON_RESPONSE -> deleteOnResponse().apply(event);
                case DEFERRABLE_EDIT -> deferrableEditResponse().apply(event);
                case DEFERRABLE_REPLY -> deferrableReplyResponse().apply(event);
            };
        };
    }

    private Mono<T> repeatInteraction() {
        return this.createMono();
    }

    private Function<T, Mono<T>> defaultResponse() {
        return event -> {
            if (event instanceof SelectMenuInteractionEvent selectMenuEvent) {
                List<LayoutComponent> components = List.of(actionRowComponent.getDisabledRow());
                return deferEditResponseThenEditWithComponents(event, selectMenuEvent, components);
            } else if (event instanceof ButtonInteractionEvent buttonEvent) {
                return deferEditResponseThenEditWithComponents(event, buttonEvent, List.of());
            } else {
                return just(event);
            }
        };
    }

    private Function<T, Mono<T>> deleteOnResponse() {
        return event -> {
            ComponentInteractionEvent interactionEvent = (ComponentInteractionEvent) event;
            return deferEditResponse(interactionEvent)
                    .then(deleteInitialResponse(interactionEvent))
                    .then(just(event));
        };
    }

    private Function<T, Mono<T>> deferrableEditResponse() {
        return event -> {
            ComponentInteractionEvent interactionEvent = (ComponentInteractionEvent) event;
            return deferEditResponse(interactionEvent)
                    .then(just(event));
        };
    }

    private Function<T, Mono<T>> deferrableReplyResponse() {
        return event -> {
            ComponentInteractionEvent interactionEvent = (ComponentInteractionEvent) event;
            return deferReplyResponse(interactionEvent)
                    .then(just(event));
        };
    }

    private Mono<T> deferEditResponseThenEditWithComponents(T event, ComponentInteractionEvent interactionEvent, List<LayoutComponent> components) {
        return deferEditResponse(interactionEvent)
                .then(editResponseWithComponents(interactionEvent, components))
                .then(just(event));
    }

    private InteractionCallbackSpecDeferEditMono deferEditResponse(ComponentInteractionEvent interactionEvent) {
        return interactionEvent.deferEdit();
    }

    private InteractionCallbackSpecDeferReplyMono deferReplyResponse(ComponentInteractionEvent interactionEvent) {
        return interactionEvent.deferReply();
    }

    private Mono<Message> editResponseWithComponents(ComponentInteractionEvent interactionEvent, List<LayoutComponent> layoutComponents) {
        return interactionEvent.editReply(InteractionReplyEditSpec.builder()
                .components(layoutComponents)
                .build());
    }

    private static <T extends Event> Mono<T> just(T event) {
        return reactor.core.publisher.Mono.just(event);
    }

    private static Mono<Void> deleteInitialResponse(ComponentInteractionEvent selectMenuEvent) {
        return selectMenuEvent.getInteractionResponse().deleteInitialResponse();
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
        private ActionRowComponent actionRowComponent;
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

        public Builder<T> actionRowComponent(ActionRowComponent actionRowComponent) {
            this.actionRowComponent = actionRowComponent;
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

        public TextPromptBuilder<T> build() {
            validate();
            return new TextPromptBuilder<>(this);
        }

        private void validate() {
            validateRequiredFields();
            validateEventPromptType();
            validateEventsRequiringComponents();
        }

        private void validateRequiredFields() {
            Objects.requireNonNull(messageChannel, "messageChannel is required");
            Objects.requireNonNull(messageCreateSpec, "messageCreateSpec is required");
            Objects.requireNonNull(eventProcessor, "eventProcessor is required");
            Objects.requireNonNull(eventPredicate, "eventPredicate is required");
            Objects.requireNonNull(promptType, "promptType is required");
        }

        private void validateEventPromptType() {
            boolean isPromptTypeValid = supportedEventOperations.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().isAssignableFrom(eventClass))
                    .flatMap(entry -> entry.getValue().stream())
                    .anyMatch(type -> type == promptType);
            if (!isPromptTypeValid) {
                throw new IllegalStateException("%s not supported operation for %s".formatted(promptType, eventClass));
            }
        }

        private void validateEventsRequiringComponents() {
            List<Class<? extends ComponentInteractionEvent>> eventsRequiringComponents = List.of(SelectMenuInteractionEvent.class);
            if (promptType.equals(PromptType.DEFAULT) && eventsRequiringComponents.contains(eventClass)) {
                Objects.requireNonNull(actionRowComponent, "actionRowComponent is required");
            }
        }
    }
}