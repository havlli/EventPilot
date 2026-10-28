package com.github.havlli.EventPilot.entity.event;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventDAO eventDAO;

    public EventService(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    public void saveEvent(Event event) {
        eventDAO.saveEvent(event);
    }

    public void deleteAllEvents(List<Event> events) {
        eventDAO.deleteAllEvents(events);
    }

    public boolean deleteEventIfExists(String id) {
        if (!eventDAO.existsById(id)) {
            return false;
        }
        eventDAO.deleteById(id);
        return true;
    }

    public List<Event> getExpiredEvents() {
        return eventDAO.getExpiredEvents();
    }

    public List<Event> getReminderCandidates(Duration reminderLeadTime) {
        return eventDAO.getReminderCandidates(Instant.now().plus(reminderLeadTime));
    }

    public Optional<Event> updateStatusIfExists(String id, EventStatus status) {
        Optional<Event> eventOptional = eventDAO.findById(id);
        if (eventOptional.isEmpty()) {
            return Optional.empty();
        }

        Event event = eventOptional.orElseThrow();
        event.setStatus(status);
        eventDAO.saveEvent(event);
        return Optional.of(event);
    }

    public boolean markExpiredIfExists(String id) {
        return updateStatusIfExists(id, EventStatus.EXPIRED).isPresent();
    }

    public boolean markReminderSentIfExists(String id) {
        Optional<Event> eventOptional = eventDAO.findById(id);
        if (eventOptional.isEmpty()) {
            return false;
        }

        Event event = eventOptional.orElseThrow();
        event.setReminderSent(true);
        eventDAO.saveEvent(event);
        return true;
    }

    public List<Event> getEventsForGuild(String guildId, List<EventStatus> statuses, int limit) {
        return eventDAO.getEventsForGuild(guildId, statuses, limit);
    }

    public Optional<Event> getEventByIdForGuild(String id, String guildId) {
        return eventDAO.findByIdAndGuildId(id, guildId);
    }

}
