package com.github.havlli.EventPilot.entity.event;

import java.util.HashSet;

public interface EventDAO {
    void insertEvent(Event event);
    HashSet<Event> fetchEvents();
}
