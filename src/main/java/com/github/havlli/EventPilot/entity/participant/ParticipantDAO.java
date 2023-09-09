package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;

import java.util.List;

public interface ParticipantDAO {
    List<Participant> getParticipantsByEvent(Event event);
    void saveParticipant(Participant participant);
}
