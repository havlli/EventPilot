package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventDTO;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        List<EventDTO> eventDTOList = eventService.getAllEvents()
                .stream()
                .map(EventDTO::fromEvent)
                .toList();

        return ResponseEntity.ok()
                .body(eventDTOList);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEventById(@PathVariable String id) {
        eventService.deleteEventById(id);

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping("/last")
    public ResponseEntity<List<EventDTO>> getLastFiveEvents() {
        List<EventDTO> eventDTOList = eventService.getLastFiveEvents()
                .stream()
                .map(EventDTO::fromEvent)
                .toList();

        return ResponseEntity.ok()
                .body(eventDTOList);
    }

    @PostMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable String id, @RequestBody EventUpdateRequest updateRequest) {
        Event updatedEvent = eventService.updateEvent(id, updateRequest);

        return ResponseEntity.ok()
                .body(EventDTO.fromEvent(updatedEvent));
    }
}
