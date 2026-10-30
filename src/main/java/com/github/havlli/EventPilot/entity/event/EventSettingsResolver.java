package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.core.DiscordProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EventSettingsResolver {

    public static final int DEFAULT_REMINDER_LEAD_MINUTES = 60;
    private final DiscordProperties discordProperties;

    public EventSettingsResolver(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
    }

    public Duration reminderLead(Event event) {
        return event.getReminderLeadMinutes() == null
                ? instanceReminderLead()
                : Duration.ofMinutes(event.getReminderLeadMinutes());
    }

    public Duration instanceReminderLead() {
        if (discordProperties == null || discordProperties.scheduler() == null || discordProperties.scheduler().reminderLeadMinutes() < 1) {
            return Duration.ofMinutes(DEFAULT_REMINDER_LEAD_MINUTES);
        }
        return Duration.ofMinutes(discordProperties.scheduler().reminderLeadMinutes());
    }
}
