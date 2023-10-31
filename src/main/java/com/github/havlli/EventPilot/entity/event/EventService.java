package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
