package com.github.havlli.EventPilot.entity.participant;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParticipantService {

    public Participant getParticipant(String id, List<Participant> participants) {
        return participants.stream()
                .filter(participant -> participant.getUserId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public void addParticipant(Participant participant, List<Participant> participants) {
        participants.add(participant);
    }

    public void updateParticipant(Participant participant, String eventId, Integer roleIndex) {
        participant.setRoleIndex(roleIndex);
    }
}
