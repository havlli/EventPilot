package com.github.havlli.EventPilot.api;

import com.github.havlli.EventPilot.entity.event.EventDTO;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
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
    public List<EventDTO> getAllEvents() {
        return eventService.getAllEvents()
                .stream()
                .map(EventDTO::fromEvent)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEventById(@PathVariable String id) {
        try {
            eventService.deleteEventById(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            ErrorResponse errorResponse = ErrorResponse.builder(e, HttpStatus.NOT_FOUND, e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse);
        }
    }
}
