package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ParticipantJPADataAccessService implements ParticipantDAO {

    private final ParticipantRepository participantRepository;

    public ParticipantJPADataAccessService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Override
    public List<Participant> getParticipantsByEvent(Event event) {
        return participantRepository.findAllByEvent(event);
    }

    @Override
    public void saveParticipant(Participant participant) {
        participantRepository.save(participant);
    }
}
