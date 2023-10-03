package com.github.havlli.EventPilot.entity.event;

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
        eventDAO.deleteById(id);
    }

    public List<Event> getExpiredEvents() {
        return eventDAO.getExpiredEvents();
    }
}
