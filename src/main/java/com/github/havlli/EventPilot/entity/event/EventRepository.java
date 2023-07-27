package com.github.havlli.EventPilot.entity.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    @Query("SELECT e FROM Event e WHERE e.dateTime < CURRENT_TIMESTAMP")
    List<Event> findAllWithDatetimeBeforeCurrentTime();
}
