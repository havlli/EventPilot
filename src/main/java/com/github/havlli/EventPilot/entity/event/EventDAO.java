package com.github.havlli.EventPilot.entity.event;

import java.util.List;

public interface EventDAO {
    void saveEvent(Event event);
    List<Event> getEvents();
    void deleteEvent(Event event);
    void deleteById(String id);
    void deleteAllEvents(List<Event> events);
    List<Event> getExpiredEvents();
}
