package com.github.havlli.EventPilot.entity.event;

import java.util.List;
import java.util.Optional;

public interface EventDAO {
    void saveEvent(Event event);
    List<Event> getEvents();
    void deleteEvent(Event event);
    void deleteById(String id);
    void deleteAllEvents(List<Event> events);
    List<Event> getExpiredEvents();
    boolean existsById(String id);
    List<Event> getLastFiveEvents();
    Optional<Event> findById(String id);
}
