package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventStatus;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class CancelEventCommand extends EventLifecycleCommand {

    public CancelEventCommand(
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource,
            EventService eventService,
            DiscordService discordService
    ) {
        super(
                "cancel-event",
                EventStatus.CANCELLED,
                "interaction.lifecycle.cancelled",
                permissionChecker,
                userSessionValidator,
                messageSource,
                eventService,
                discordService
        );
    }
}
