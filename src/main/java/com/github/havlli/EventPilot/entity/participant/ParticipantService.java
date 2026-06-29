package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ParticipantService {

    private final ParticipantDAO participantDAO;
    private final EmbedGenerator embedGenerator;
    private final EventService eventService;
    private final GatewayDiscordClient client;

    public ParticipantService(
            ParticipantDAO participantDAO,
            EmbedGenerator embedGenerator,
            EventService eventService,
            GatewayDiscordClient client
    ) {
        this.participantDAO = participantDAO;
        this.embedGenerator = embedGenerator;
        this.eventService = eventService;
        this.client = client;
    }

    public Optional<Participant> getParticipant(String id, List<Participant> participants) {
        return participants.stream()
                .filter(participant -> participant.getUserId().equals(id))
                .findFirst();
    }

    public void addParticipant(Participant participant, List<Participant> participants) {
        participants.add(participant);
    }

    public void updateRoleIndex(Participant participant, Integer roleIndex) {
        participant.setRoleIndex(roleIndex);
    }

    public List<Participant> getParticipantsByEvent(Event event) {
        return participantDAO.getParticipantsByEvent(event);
    }

    public boolean removeParticipantFromDiscordEvent(Long participantId) {
        Optional<Participant> participant = participantDAO.findById(participantId);
        if (participant.isPresent()) {
            Event event = removeParticipantAndUpdateEvent(participant.orElseThrow());

            updateDiscordMessage(event).subscribe();

            return true;
        } else
            return false;
    }

    protected Mono<Message> updateDiscordMessage(Event event) {
        return client.getMessageById(Snowflake.of(event.getDestinationChannelId()), Snowflake.of(event.getEventId()))
                .flatMap(message -> message.edit(MessageEditSpec.builder()
                        .addEmbed(embedGenerator.generateEmbed(event))
                        .build()))
                .onErrorComplete();
    }

    private Event removeParticipantAndUpdateEvent(Participant participant) {
        Event.Builder eventBuilder = Event.builder()
                .fromEvent(participant.getEvent());

        List<Participant> updatedParticipants = new ArrayList<>(eventBuilder.getParticipants());
        updatedParticipants.remove(participant);
        promoteWaitlistedParticipants(updatedParticipants, participant.getEvent().getMemberSize());
        eventBuilder.withParticipants(updatedParticipants);

        Event event = eventBuilder.build();
        eventService.saveEvent(event);

        return event;
    }

    private void promoteWaitlistedParticipants(List<Participant> participants, String memberSize) {
        Optional<Integer> capacity = parseCapacity(memberSize);
        if (capacity.isEmpty()) {
            return;
        }

        while (signedUpParticipantCount(participants) < capacity.orElseThrow()) {
            Optional<Participant> participantToPromote = nextWaitlistedParticipant(participants);
            if (participantToPromote.isEmpty()) {
                return;
            }

            participantToPromote.orElseThrow().setStatus(ParticipantStatus.SIGNED_UP);
        }
    }

    private Optional<Integer> parseCapacity(String memberSize) {
        try {
            int capacity = Integer.parseInt(memberSize);
            return capacity > 0 ? Optional.of(capacity) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
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
