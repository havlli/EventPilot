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
    public void saveEvent(Event event) {
        eventRepository.save(event);
    }

    @Override
    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    @Override
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }

    @Override
    public void deleteById(String id) {
        eventRepository.deleteById(id);
    }

    @Override
    public void deleteAllEvents(List<Event> events) {
        eventRepository.deleteAll(events);
    }

    @Override
    public List<Event> getExpiredEvents() {
        return eventRepository.findAllWithDatetimeBeforeCurrentTime();
    }

    @Override
    public boolean existsById(String id) {
        return eventRepository.existsById(id);
    }
}
