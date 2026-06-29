package com.github.havlli.EventPilot.command;

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
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Permission;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ListEventsCommand implements SlashCommand {

    private static final String EVENT_NAME = "list-events";
    private static final String OPTION_STATUS = "status";
    private static final String OPTION_LIMIT = "limit";
    private static final String DEFAULT_STATUS = "active";
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAXIMUM_LIMIT = 10;
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;
    private final SimplePermissionValidator permissionChecker;
    private final UserSessionValidator userSessionValidator;
    private final MessageSource messageSource;
    private final EventService eventService;
    private final OrganizerEventFormatter organizerEventFormatter;

    public ListEventsCommand(
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource,
            EventService eventService,
            OrganizerEventFormatter organizerEventFormatter
    ) {
        this.permissionChecker = permissionChecker;
        this.userSessionValidator = userSessionValidator;
        this.messageSource = messageSource;
        this.eventService = eventService;
        this.organizerEventFormatter = organizerEventFormatter;
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
        if (!interactionEvent.getCommandName().equals(EVENT_NAME)) {
            return Mono.empty();
        }

        return deferInteractionWithEphemeralResponse(interactionEvent)
                .then(validatePermissions(interactionEvent));
    }

    public Mono<Message> listEventsInteraction(ChatInputInteractionEvent event) {
        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            return sendMessage(event, messageSource.getMessage("interaction.organizer.guild-only", null, Locale.ENGLISH));
        }

        List<EventStatus> statuses = statusesFor(statusOption(event));
        int limit = limitOption(event);

        return Mono.fromSupplier(() -> eventService.getEventsForGuild(guildId.orElseThrow().asString(), statuses, limit))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(events -> {
                    if (events.isEmpty()) {
                        return sendMessage(event, messageSource.getMessage("interaction.list-events.no-events", null, Locale.ENGLISH));
                    }

                    return sendMessage(event, organizerEventFormatter.formatEventList(events));
                });
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent event) {
        return event.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionChecker.followupWith(validateSession(event), event, Permission.MANAGE_CHANNELS);
    }

    private Mono<Message> validateSession(ChatInputInteractionEvent event) {
        return userSessionValidator.validateThenWrap(listEventsInteraction(event), event);
    }

    private String statusOption(ChatInputInteractionEvent event) {
        return event.getOption(OPTION_STATUS)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> value.asString().toLowerCase(Locale.ROOT))
                .orElse(DEFAULT_STATUS);
    }

    private int limitOption(ChatInputInteractionEvent event) {
        int requestedLimit = event.getOption(OPTION_LIMIT)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> (int) value.asLong())
                .orElse(DEFAULT_LIMIT);

        return Math.max(1, Math.min(MAXIMUM_LIMIT, requestedLimit));
    }

    private List<EventStatus> statusesFor(String status) {
        return switch (status) {
            case "open" -> List.of(EventStatus.OPEN);
            case "closed" -> List.of(EventStatus.CLOSED);
            case "cancelled" -> List.of(EventStatus.CANCELLED);
            case "expired" -> List.of(EventStatus.EXPIRED);
            case "all" -> List.of();
            case "active" -> List.of(EventStatus.OPEN, EventStatus.CLOSED);
            default -> List.of(EventStatus.OPEN, EventStatus.CLOSED);
        };
    }

    private Mono<Message> sendMessage(ChatInputInteractionEvent event, String content) {
        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                .content(content)
                .ephemeral(true)
                .build());
    }
}
