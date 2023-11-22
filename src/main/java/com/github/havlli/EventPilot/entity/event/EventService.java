package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventDAO eventDAO;

    public EventService(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    public void saveEvent(Event event) {
        eventDAO.saveEvent(event);
    }

    public List<Event> getAllEvents() {
        return eventDAO.getEvents();
    }

    public void deleteEvent(Event event) {
        eventDAO.deleteEvent(event);
    }

    public void deleteAllEvents(List<Event> events) {
        eventDAO.deleteAllEvents(events);
    }

    public void deleteEventById(String id) {
        if (!eventDAO.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete event with id {%s} - does not exist!".formatted(id));
        }
        eventDAO.deleteById(id);
    }

    public List<Event> getExpiredEvents() {
        return eventDAO.getExpiredEvents();
    }

    public List<Event> getLastFiveEvents() {
        return eventDAO.getLastFiveEvents();
    }

    public Event updateEvent(String id, EventUpdateRequest updateRequest) {
        Optional<Event> eventOptional = eventDAO.findById(id);

        if (eventOptional.isEmpty()) {
            throw new ResourceNotFoundException("Cannot update event with id {%s} - does not exist!".formatted(id));
        }

        Event updatedEvent = updateRequest.updateEvent(eventOptional.orElseThrow());

        eventDAO.saveEvent(updatedEvent);

        return updatedEvent;
    }

    public Event getEventById(String id) {
        return eventDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot get event with id {%s} - does not exist!".formatted(id)));
    }
}
