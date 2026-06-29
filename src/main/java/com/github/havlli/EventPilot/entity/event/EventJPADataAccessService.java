package com.github.havlli.EventPilot.entity.event;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class EventJPADataAccessService implements EventDAO {

    private static final int MINIMUM_LIMIT = 1;
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
    public List<Event> getReminderCandidates(Instant reminderCutoff) {
        return eventRepository.findReminderCandidates(reminderCutoff);
    }

    @Override
    public boolean existsById(String id) {
        return eventRepository.existsById(id);
    }

    @Override
    public List<Event> getLastFiveEvents() {
        return eventRepository.findLastFiveEvents();
    }

    @Override
    public List<Event> getEventsForGuild(String guildId, List<EventStatus> statuses, int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.max(MINIMUM_LIMIT, limit));
        if (statuses == null || statuses.isEmpty()) {
            return eventRepository.findByGuildIdOrderByDateTimeAsc(guildId, pageRequest);
        }

        return eventRepository.findByGuildIdAndStatusInOrderByDateTimeAsc(guildId, statuses, pageRequest);
    }

    @Override
    public Optional<Event> findById(String id) {
        return eventRepository.findById(id);
    }

    @Override
    public Optional<Event> findByIdAndGuildId(String id, String guildId) {
        return eventRepository.findByIdAndGuildId(id, guildId);
    }

    @Override
    public Optional<Event> findByIdForUpdate(String id) {
        return eventRepository.findByIdForUpdate(id);
    }
}
