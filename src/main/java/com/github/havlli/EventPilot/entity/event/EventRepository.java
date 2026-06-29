package com.github.havlli.EventPilot.entity.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
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

    default List<Event> findAllWithDatetimeBeforeCurrentTime() {
        return findAllWithDatetimeBeforeCurrentTime(List.of(EventStatus.OPEN, EventStatus.CLOSED));
    }

    @Query("SELECT e FROM Event e WHERE e.dateTime < CURRENT_TIMESTAMP AND e.status IN :statuses")
    List<Event> findAllWithDatetimeBeforeCurrentTime(List<EventStatus> statuses);

    default List<Event> findReminderCandidates(Instant reminderCutoff) {
        return findReminderCandidates(reminderCutoff, List.of(EventStatus.OPEN, EventStatus.CLOSED));
    }

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            WHERE e.dateTime > CURRENT_TIMESTAMP
            AND e.dateTime <= :reminderCutoff
            AND e.status IN :statuses
            AND e.reminderSent = false
            """)
    List<Event> findReminderCandidates(Instant reminderCutoff, List<EventStatus> statuses);

    @Override
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            WHERE e.eventId = :id
            """)
    Optional<Event> findById(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            WHERE e.eventId = :id
            """)
    Optional<Event> findByIdForUpdate(String id);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.guild
            LEFT JOIN FETCH e.embedType
            ORDER BY e.eventId DESC LIMIT 5
            """)
    List<Event> findLastFiveEvents();
}
