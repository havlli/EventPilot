package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ParticipantService {

    private final ParticipantDAO participantDAO;

    public ParticipantService(ParticipantDAO participantDAO) {
        this.participantDAO = participantDAO;
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
        return participantDAO.getAllParticipantsByEvent(event);
    }
}
