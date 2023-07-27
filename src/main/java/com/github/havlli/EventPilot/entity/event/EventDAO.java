package com.github.havlli.EventPilot.entity.event;

import java.util.List;

public interface EventDAO {
    void insertEvent(Event event);
    List<Event> fetchEvents();
    void deleteEvent(Event event);
    List<Event> fetchExpiredEvents();
}
