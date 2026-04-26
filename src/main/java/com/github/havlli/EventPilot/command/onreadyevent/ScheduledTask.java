package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.core.DiscordProperties;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTask.class);
    private final Integer intervalSeconds;
    private final EventService eventService;
    private final DiscordService discordService;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Disposable schedulerSubscription;

    public ScheduledTask(
            EventService eventService,
            DiscordService discordService,
            DiscordProperties discordProperties
    ) {
        this.eventService = eventService;
        this.discordService = discordService;
        this.intervalSeconds = discordProperties.scheduler().intervalSeconds();
    }

    public Flux<Void> getSchedulersFlux() {
        return Flux.interval(Duration.ofSeconds(intervalSeconds), Duration.ofSeconds(intervalSeconds))
                .flatMap(__ -> handleExpiredEvents());
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

    private Mono<Void> handleExpiredEvents() {
        return Mono.defer(() -> getExpiredEventList()
                .flatMapMany(discordService::deactivateEvents)
                .then()
        ).onErrorResume(error -> {
            LOG.error("Expired event scheduler cycle failed", error);
            return Mono.empty();
        });
    }

    private Mono<List<Event>> getExpiredEventList() {
        return Mono.fromCallable(eventService::getExpiredEvents)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
