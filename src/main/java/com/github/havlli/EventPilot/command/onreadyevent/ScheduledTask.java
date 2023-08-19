package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Component
public class ScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class);
    private final Integer intervalSeconds;

    private final EventService eventService;
    private final DiscordService discordService;

    public ScheduledTask(
            EventService eventService,
            DiscordService discordService,
            @Value("${discord.scheduler.interval-seconds}") Integer intervalSeconds
    ) {
        this.eventService = eventService;
        this.discordService = discordService;
        this.intervalSeconds = intervalSeconds;
    }

    public Flux<Void> getFlux() {
        logger.info("MainScheduler registered");
        Scheduler scheduler = Schedulers.newSingle("MainScheduler");

        return handleExpiredEvents()
                .delaySubscription(Duration.ofSeconds(intervalSeconds))
                .repeat()
                .subscribeOn(scheduler);
    }

    private Mono<Void> handleExpiredEvents() {
        // TODO: Decide what to do with expired events in database
        //  - delete immediately or retain for some amount of time
        List<Event> expiredEvents = eventService.getExpiredEvents();
        discordService.deactivateEvents(expiredEvents);

        return Mono.empty();
    }
}
