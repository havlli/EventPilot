package com.github.havlli.EventPilot.entity.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventDAO eventDAO;

    private final EventSettingsResolver settingsResolver;

    public EventService(EventDAO eventDAO, EventSettingsResolver settingsResolver) {
        this.eventDAO = eventDAO;
        this.settingsResolver = settingsResolver;
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

    public List<Event> getReminderCandidates() {
        return getReminderCandidates(Instant.now());
    }

    List<Event> getReminderCandidates(Instant now) {
        Integer maxOverride = eventDAO.getMaxReminderLeadMinutes();
        Duration largestLead = Duration.ofMinutes(Math.max(
                settingsResolver.instanceReminderLead().toMinutes(), maxOverride == null ? 0 : maxOverride));
        return eventDAO.getReminderCandidates(now.plus(largestLead)).stream()
                .filter(event -> !event.getDateTime().isAfter(now.plus(settingsResolver.reminderLead(event))))
                .toList();
    }

    @Transactional
    public Optional<Event> updateReminderLeadMinutesForGuild(String id, String guildId, Integer minutes) {
        if (minutes != null && (minutes < 1 || minutes > 10080)) {
            throw new IllegalArgumentException("Reminder lead must be between 1 and 10080 minutes");
        }
        return eventDAO.findByIdForUpdate(id)
                .filter(event -> guildId != null && event.getGuild() != null && guildId.equals(event.getGuild().getId()))
                .filter(event -> event.getStatus() == EventStatus.OPEN || event.getStatus() == EventStatus.CLOSED)
                .map(event -> {
                    event.setReminderLeadMinutes(minutes);
                    eventDAO.saveEvent(event);
                    return event;
                });
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
