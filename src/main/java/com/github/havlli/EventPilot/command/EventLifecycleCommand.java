package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventStatus;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.rest.util.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;
import java.util.Optional;

public abstract class EventLifecycleCommand implements SlashCommand {

    private static final Logger LOG = LoggerFactory.getLogger(EventLifecycleCommand.class);
    private static final String OPTION_MESSAGE_ID = "message-id";
    private final String commandName;
    private final EventStatus targetStatus;
    private final String successMessageKey;
    private final SimplePermissionValidator permissionChecker;
    private final UserSessionValidator userSessionValidator;
    private final MessageSource messageSource;
    private final EventService eventService;
    private final DiscordService discordService;
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

    protected EventLifecycleCommand(
            String commandName,
            EventStatus targetStatus,
            String successMessageKey,
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource,
            EventService eventService,
            DiscordService discordService
    ) {
        this.commandName = commandName;
        this.targetStatus = targetStatus;
        this.successMessageKey = successMessageKey;
        this.permissionChecker = permissionChecker;
        this.userSessionValidator = userSessionValidator;
        this.messageSource = messageSource;
        this.eventService = eventService;
        this.discordService = discordService;
    }

    @Override
    public String getName() {
        return commandName;
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
        if (!interactionEvent.getCommandName().equals(commandName)) {
            return Mono.empty();
        }

        return deferInteractionWithEphemeralResponse(interactionEvent)
                .then(validatePermissions(interactionEvent));
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent event) {
        return event.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionChecker.followupWith(validateSession(event), event, Permission.MANAGE_CHANNELS);
    }

    private Mono<Message> validateSession(ChatInputInteractionEvent event) {
        return userSessionValidator.validateThenWrap(applyStateTransition(event), event);
    }

    private Mono<Message> applyStateTransition(ChatInputInteractionEvent interactionEvent) {
        String eventId = getTargetMessageId(interactionEvent).asString();
        return Mono.fromSupplier(() -> eventService.updateStatusIfExists(eventId, targetStatus))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> handleLifecycleResult(interactionEvent, result));
    }

    private Mono<Message> handleLifecycleResult(
            ChatInputInteractionEvent interactionEvent,
            Optional<com.github.havlli.EventPilot.entity.event.Event> event
    ) {
        if (event.isEmpty()) {
            return sendMessage(interactionEvent, "interaction.lifecycle.event-not-found");
        }

        com.github.havlli.EventPilot.entity.event.Event updatedEvent = event.orElseThrow();
        return discordService.updateEventMessage(updatedEvent)
                .doOnError(error -> LOG.warn("Could not refresh event message [{}] after lifecycle update", updatedEvent.getEventId(), error))
                .onErrorResume(error -> Mono.empty())
                .then(sendMessage(interactionEvent, successMessageKey));
    }

    private static Snowflake getTargetMessageId(ChatInputInteractionEvent event) {
        return event.getOption(OPTION_MESSAGE_ID)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> Snowflake.of(value.asString()))
                .orElse(Snowflake.of(0));
    }

    private Mono<Message> sendMessage(ChatInputInteractionEvent event, String messageKey) {
        String message = messageSource.getMessage(messageKey, null, Locale.ENGLISH);
        return event.createFollowup(message)
                .withEphemeral(true);
    }
}
