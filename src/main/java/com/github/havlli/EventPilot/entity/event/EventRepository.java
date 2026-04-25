package com.github.havlli.EventPilot.entity.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, String> {
    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            """)
    List<Event> findAll();

    @Query("SELECT e FROM Event e WHERE e.dateTime < CURRENT_TIMESTAMP")
    List<Event> findAllWithDatetimeBeforeCurrentTime();

    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            WHERE e.eventId = :id
            """)
    Optional<Event> findById(String id);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            ORDER BY e.eventId DESC LIMIT 5
            """)
    List<Event> findLastFiveEvents();
}
