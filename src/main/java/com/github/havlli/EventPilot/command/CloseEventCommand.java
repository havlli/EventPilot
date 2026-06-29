package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventStatus;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class CloseEventCommand extends EventLifecycleCommand {

    public CloseEventCommand(
            SimplePermissionValidator permissionChecker,
            UserSessionValidator userSessionValidator,
            MessageSource messageSource,
            EventService eventService,
            DiscordService discordService
    ) {
        super(
                "close-event",
                EventStatus.CLOSED,
                "interaction.lifecycle.closed",
                permissionChecker,
                userSessionValidator,
                messageSource,
                eventService,
                discordService
        );
    }
}
