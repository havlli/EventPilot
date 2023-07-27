package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Component
public class ScheduledTask {

    @Value("${discord.scheduler.interval-seconds}")
    private Integer INTERVAL_SECONDS;

    private final EventService eventService;
    private final DiscordService discordService;

    public ScheduledTask(EventService eventService, DiscordService discordService) {
        this.eventService = eventService;
        this.discordService = discordService;
    }

    public Mono<Void> getMono() {
        Duration duration = Duration.ofSeconds(INTERVAL_SECONDS);

        System.out.println("Scheduler registered");
        Scheduler scheduler = Schedulers.newSingle("MainScheduler");

        return Mono.fromRunnable(this::handleExpiredEvents)
                .delaySubscription(duration)
                .repeat()
                .subscribeOn(scheduler)
                .then();
    }

    private void handleExpiredEvents() {
        // TODO: Decide what to do with expired events in database
        //  - delete immediately or retain for some amount of time
        List<Event> expiredEvents = eventService.getExpiredEvents();
        discordService.deactivateEvents(expiredEvents);
    }
}
