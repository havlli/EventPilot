package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.EventService;
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

import java.util.Locale;
import java.util.Optional;

@Component
public class EventInfoCommand implements SlashCommand {

    private static final String EVENT_NAME = "event-info";
    private static final String OPTION_MESSAGE_ID = "message-id";
    private Class<? extends Event> eventType = ChatInputInteractionEvent.class;
    private final SimplePermissionValidator permissionChecker;
    private final UserSessionValidator userSessionValidator;
    private final MessageSource messageSource;
    private final EventService eventService;
    private final OrganizerEventFormatter organizerEventFormatter;

    public EventInfoCommand(
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

    public Mono<Message> eventInfoInteraction(ChatInputInteractionEvent event) {
        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            return sendMessage(event, messageSource.getMessage("interaction.organizer.guild-only", null, Locale.ENGLISH));
        }

        String eventId = targetMessageId(event);

        return Mono.fromSupplier(() -> eventService.getEventByIdForGuild(eventId, guildId.orElseThrow().asString()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(eventOptional -> eventOptional
                        .map(foundEvent -> sendMessage(event, organizerEventFormatter.formatEventDetails(foundEvent)))
                        .orElseGet(() -> sendMessage(event, messageSource.getMessage("interaction.event-info.event-not-found", null, Locale.ENGLISH))));
    }

    private InteractionCallbackSpecDeferReplyMono deferInteractionWithEphemeralResponse(ChatInputInteractionEvent event) {
        return event.deferReply()
                .withEphemeral(true);
    }

    private Mono<Message> validatePermissions(ChatInputInteractionEvent event) {
        return permissionChecker.followupWith(validateSession(event), event, Permission.MANAGE_CHANNELS);
    }

    private Mono<Message> validateSession(ChatInputInteractionEvent event) {
        return userSessionValidator.validateThenWrap(eventInfoInteraction(event), event);
    }

    private String targetMessageId(ChatInputInteractionEvent event) {
        return event.getOption(OPTION_MESSAGE_ID)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> Snowflake.of(value.asString()).asString())
                .orElse(Snowflake.of(0).asString());
    }

    private Mono<Message> sendMessage(ChatInputInteractionEvent event, String content) {
        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                .content(content)
                .ephemeral(true)
                .build());
    }
}
