package com.github.havlli.EventPilot.entity.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EventSignupService {

    private final EventDAO eventDAO;
    private final EmbedTypeService embedTypeService;

    public EventSignupService(EventDAO eventDAO, EmbedTypeService embedTypeService) {
        this.eventDAO = eventDAO;
        this.embedTypeService = embedTypeService;
    }

    @Transactional
    public EventSignupResult applySignup(String eventId, String userId, String username, int roleIndex) {
        Optional<Event> eventOptional = eventDAO.findByIdForUpdate(eventId);
        if (eventOptional.isEmpty()) {
            return EventSignupResult.withoutEvent(EventSignupResult.Outcome.EVENT_NOT_FOUND);
        }

        Event event = eventOptional.orElseThrow();
        Optional<EventSignupResult.Outcome> statusOutcome = statusOutcome(event);
        if (statusOutcome.isPresent()) {
            return EventSignupResult.withoutEvent(statusOutcome.orElseThrow());
        }

        if (!isKnownRole(event, roleIndex)) {
            return EventSignupResult.withoutEvent(EventSignupResult.Outcome.ROLE_NOT_FOUND);
        }

        Optional<Integer> capacity = parseCapacity(event);
        if (capacity.isEmpty()) {
            return EventSignupResult.withoutEvent(EventSignupResult.Outcome.INVALID_CAPACITY);
        }

        List<Participant> participants = event.getParticipants();
        List<Participant> existingParticipants = participants.stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .toList();

        if (!existingParticipants.isEmpty()) {
            ParticipantStatus signupStatus = statusForExistingSignup(participants, userId, roleIndex, capacity.orElseThrow());
            existingParticipants.forEach(participant -> {
                participant.setRoleIndex(roleIndex);
                participant.setStatus(signupStatus);
            });
            promoteWaitlistedParticipants(event, capacity.orElseThrow());
            eventDAO.saveEvent(event);
            return signupStatus == ParticipantStatus.WAITLISTED
                    ? EventSignupResult.waitlisted(event)
                    : EventSignupResult.updated(event);
        }

        Participant participant = new Participant(userId, username, nextPosition(participants), roleIndex, event);
        ParticipantStatus signupStatus = statusForNewSignup(participants, roleIndex, capacity.orElseThrow());
        participant.setStatus(signupStatus);
        participants.add(participant);
        eventDAO.saveEvent(event);
        return signupStatus == ParticipantStatus.WAITLISTED
                ? EventSignupResult.waitlisted(event)
                : EventSignupResult.added(event);
    }

    private Optional<EventSignupResult.Outcome> statusOutcome(Event event) {
        EventStatus status = event.getStatus() == null ? EventStatus.OPEN : event.getStatus();
        return switch (status) {
            case OPEN -> Optional.empty();
            case CLOSED -> Optional.of(EventSignupResult.Outcome.EVENT_CLOSED);
            case CANCELLED -> Optional.of(EventSignupResult.Outcome.EVENT_CANCELLED);
            case EXPIRED -> Optional.of(EventSignupResult.Outcome.EVENT_EXPIRED);
        };
    }

    private boolean isKnownRole(Event event, int roleIndex) {
        try {
            return embedTypeService.getDeserializedMap(event.getEmbedType()).containsKey(roleIndex);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private Optional<Integer> parseCapacity(Event event) {
        try {
            int capacity = Integer.parseInt(event.getMemberSize());
            return capacity > 0 ? Optional.of(capacity) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private int nextPosition(List<Participant> participants) {
        return participants.stream()
                .map(Participant::getPosition)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private ParticipantStatus statusForExistingSignup(
            List<Participant> participants,
            String userId,
            int roleIndex,
            int capacity
    ) {
        if (!countsTowardCapacity(roleIndex)) {
            return ParticipantStatus.SIGNED_UP;
        }

        if (alreadyConsumesCapacity(participants, userId)) {
            return ParticipantStatus.SIGNED_UP;
        }

        return signedUpParticipantCount(participants) < capacity
                ? ParticipantStatus.SIGNED_UP
                : ParticipantStatus.WAITLISTED;
    }

    private ParticipantStatus statusForNewSignup(List<Participant> participants, int roleIndex, int capacity) {
        if (!countsTowardCapacity(roleIndex)) {
            return ParticipantStatus.SIGNED_UP;
        }

        return signedUpParticipantCount(participants) < capacity
                ? ParticipantStatus.SIGNED_UP
                : ParticipantStatus.WAITLISTED;
    }

    private boolean alreadyConsumesCapacity(List<Participant> participants, String userId) {
        return participants.stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .anyMatch(this::countsTowardCapacity);
    }

    private void promoteWaitlistedParticipants(Event event, int capacity) {
        while (signedUpParticipantCount(event.getParticipants()) < capacity) {
            Optional<Participant> participantToPromote = nextWaitlistedParticipant(event.getParticipants());
            if (participantToPromote.isEmpty()) {
                return;
            }

            participantToPromote.orElseThrow().setStatus(ParticipantStatus.SIGNED_UP);
        }
    }

    private Optional<Participant> nextWaitlistedParticipant(List<Participant> participants) {
        return participants.stream()
                .filter(this::isWaitlisted)
                .filter(participant -> countsTowardCapacity(participant.getRoleIndex()))
                .min(Comparator.comparing(Participant::getPosition));
    }

    private long signedUpParticipantCount(List<Participant> participants) {
        return participants.stream()
                .filter(this::countsTowardCapacity)
                .count();
    }

    private boolean countsTowardCapacity(Participant participant) {
        ParticipantStatus status = participant.getStatus() == null
                ? ParticipantStatus.SIGNED_UP
                : participant.getStatus();
        return status == ParticipantStatus.SIGNED_UP && countsTowardCapacity(participant.getRoleIndex());
    }

    private boolean countsTowardCapacity(Integer roleIndex) {
        return roleIndex != null && roleIndex > 0;
    }

    private boolean isWaitlisted(Participant participant) {
        return participant.getStatus() == ParticipantStatus.WAITLISTED;
    }
}
