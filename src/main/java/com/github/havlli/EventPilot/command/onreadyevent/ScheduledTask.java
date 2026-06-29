package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.core.DiscordProperties;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTask.class);
    private final Duration interval;
    private final Duration reminderLeadTime;
    private final Scheduler timerScheduler;
    private final Scheduler blockingScheduler;
    private final EventService eventService;
    private final DiscordService discordService;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Disposable schedulerSubscription;

    @Autowired
    public ScheduledTask(
            EventService eventService,
            DiscordService discordService,
            DiscordProperties discordProperties
    ) {
        this.eventService = eventService;
        this.discordService = discordService;
        this.interval = Duration.ofSeconds(discordProperties.scheduler().intervalSeconds());
        this.reminderLeadTime = Duration.ofMinutes(discordProperties.scheduler().reminderLeadMinutes());
        this.timerScheduler = Schedulers.parallel();
        this.blockingScheduler = Schedulers.boundedElastic();
    }

    ScheduledTask(
            EventService eventService,
            DiscordService discordService,
            Duration interval,
            Duration reminderLeadTime,
            Scheduler timerScheduler,
            Scheduler blockingScheduler
    ) {
        this.eventService = eventService;
        this.discordService = discordService;
        this.interval = interval;
        this.reminderLeadTime = reminderLeadTime;
        this.timerScheduler = timerScheduler;
        this.blockingScheduler = blockingScheduler;
    }

    public Flux<Void> getSchedulersFlux() {
        return Flux.interval(interval, interval, timerScheduler)
                .flatMap(__ -> handleScheduledEvents());
    }

    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            if (started.compareAndSet(false, true)) {
                schedulerSubscription = getSchedulersFlux()
                        .doFinally(__ -> started.set(false))
                        .subscribe();
            }
        });
    }

    @PreDestroy
    public void stop() {
        if (schedulerSubscription != null && !schedulerSubscription.isDisposed()) {
            schedulerSubscription.dispose();
        }
    }

    private Mono<Void> handleScheduledEvents() {
        return handleExpiredEvents()
                .then(handleEventReminders())
                .onErrorResume(error -> {
                    LOG.error("Scheduled event cycle failed", error);
                    return Mono.empty();
                });
    }

    private Mono<Void> handleExpiredEvents() {
        return getExpiredEventList()
                .flatMapMany(discordService::deactivateEvents)
                .then();
    }

    private Mono<Void> handleEventReminders() {
        return getReminderEventList()
                .flatMapMany(discordService::sendEventReminders)
                .then();
    }

    private Mono<List<Event>> getExpiredEventList() {
        return Mono.fromCallable(eventService::getExpiredEvents)
                .subscribeOn(blockingScheduler);
    }

    private Mono<List<Event>> getReminderEventList() {
        return Mono.fromCallable(() -> eventService.getReminderCandidates(reminderLeadTime))
                .subscribeOn(blockingScheduler);
    }
}
