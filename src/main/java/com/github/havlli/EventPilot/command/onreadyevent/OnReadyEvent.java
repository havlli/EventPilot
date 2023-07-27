package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class OnReadyEvent implements SlashCommand {

    private final StartupTask startupTask;
    private final ScheduledTask scheduledTask;

    public OnReadyEvent(StartupTask startupTask, ScheduledTask scheduledTask) {
        this.startupTask = startupTask;
        this.scheduledTask = scheduledTask;
    }

    private Class<? extends Event> eventType = ReadyEvent.class;

    @Override
    public String getName() {
        return "on-ready";
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
        return startupTask.handleNewGuilds()
                .then(startupTask.subscribeEventInteractions())
                .then(scheduledTask.getMono());
    }
}
