package com.github.havlli.EventPilot.command.onreadyevent;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class StartupTask {

    private final EventService eventService;
    private final EmbedGenerator embedGenerator;

    public StartupTask(EventService eventService, EmbedGenerator embedGenerator) {
        this.eventService = eventService;
        this.embedGenerator = embedGenerator;
    }

    public Mono<Void> subscribeEventInteractions() {
        List<Event> events = eventService.getAllEvents();
        events.forEach(embedGenerator::subscribeInteractions);
        System.out.println("interactions subscribed");
        return Mono.empty();
    }
}
