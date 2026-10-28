package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventSettingsResolver;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Component
public class EventOptionsCommand extends OrganizerCommand {

    private final EventService eventService;
    private final EventSettingsResolver settingsResolver;

    public EventOptionsCommand(
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource,
            EventService eventService,
            EventSettingsResolver settingsResolver
    ) {
        super("event-options", permissionChecker, userSessionValidator, messageSource);
        this.eventService = eventService;
        this.settingsResolver = settingsResolver;
    }

    @Override
    protected Mono<Message> processInteraction(ChatInputInteractionEvent interaction) {
        return Mono.defer(() -> {
            Optional<Snowflake> guildId = getGuildId(interaction);
            if (guildId.isEmpty()) {
                return sendMessage(interaction, getMessage("interaction.organizer.guild-only"));
            }

            Optional<Long> minutes = interaction.getOption("reminder-minutes")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asLong);
            boolean reset = interaction.getOption("reset")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asBoolean)
                    .orElse(false);
            if (reset && minutes.isPresent()) {
                return sendMessage(interaction, getMessage("interaction.event-options.conflicting-options"));
            }
            if (minutes.filter(value -> value < 1 || value > 10080).isPresent()) {
                return sendMessage(interaction, getMessage("interaction.event-options.invalid-minutes"));
            }

            String eventId = targetMessageId(interaction);
            String guild = guildId.orElseThrow().asString();
            boolean changing = reset || minutes.isPresent();
            return Mono.fromSupplier(() -> changing
                            ? eventService.updateReminderLeadMinutesForGuild(eventId, guild,
                                    minutes.map(Long::intValue).orElse(null))
                            : eventService.getEventByIdForGuild(eventId, guild))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(result -> result
                            .map(event -> sendMessage(interaction, formatOptions(event, changing)))
                            .orElseGet(() -> sendMessage(interaction, getMessage(changing
                                    ? "interaction.event-options.unavailable"
                                    : "interaction.event-info.event-not-found"))));
        });
    }

    private String formatOptions(Event event, boolean changed) {
        String source = getMessage(event.getReminderLeadMinutes() == null
                ? "interaction.event-options.instance-default"
                : "interaction.event-options.override");
        String sent = getMessage(event.isReminderSent()
                ? "interaction.event-options.sent" : "interaction.event-options.not-sent");
        String options = getMessage("interaction.event-options.current",
                Long.toString(settingsResolver.reminderLead(event).toMinutes()), source, sent);
        if (changed) {
            options = getMessage("interaction.event-options.saved") + "\n" + options;
            if (event.isReminderSent()) {
                options += "\n" + getMessage("interaction.event-options.already-sent");
            }
        }
        return options;
    }
}
