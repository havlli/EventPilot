package com.github.havlli.EventPilot.entity.event;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EventJPADataAccessService implements EventDAO {

    private final EventRepository eventRepository;

    public EventJPADataAccessService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void insertEvent(Event event) {
        eventRepository.save(event);
    }

    @Override
    public List<Event> fetchEvents() {
        return eventRepository.findAll();
    }

    @Override
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }
}
