package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Message;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Component
public class EventInfoCommand extends OrganizerCommand {

    private static final String EVENT_NAME = "event-info";
    private static final String OPTION_MESSAGE_ID = "message-id";
    private final EventService eventService;
    private final OrganizerEventFormatter organizerEventFormatter;

    public EventInfoCommand(
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource,
            EventService eventService,
            OrganizerEventFormatter organizerEventFormatter
    ) {
        super(EVENT_NAME, permissionChecker, userSessionValidator, messageSource);
        this.eventService = eventService;
        this.organizerEventFormatter = organizerEventFormatter;
    }

    @Override
    protected Mono<Message> processInteraction(ChatInputInteractionEvent event) {
        return eventInfoInteraction(event);
    }

    public Mono<Message> eventInfoInteraction(ChatInputInteractionEvent event) {
        Optional<Snowflake> guildId = getGuildId(event);
        if (guildId.isEmpty()) {
            return sendMessage(event, getMessage("interaction.organizer.guild-only"));
        }

        String eventId = targetMessageId(event);

        return Mono.fromSupplier(() -> eventService.getEventByIdForGuild(eventId, guildId.orElseThrow().asString()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(eventOptional -> eventOptional
                        .map(foundEvent -> sendMessage(event, organizerEventFormatter.formatEventDetails(foundEvent)))
                        .orElseGet(() -> sendMessage(event, getMessage("interaction.event-info.event-not-found"))));
    }

    private String targetMessageId(ChatInputInteractionEvent event) {
        return event.getOption(OPTION_MESSAGE_ID)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(value -> Snowflake.of(value.asString()).asString())
                .orElse(Snowflake.of(0).asString());
    }
}
