package com.github.havlli.EventPilot.entity.participant;

import java.util.List;

public record ParticipantDTO(
        Long id,
        String discordUserId,
        String username,
        Integer position,
        Integer roleIndex
) {
    public static ParticipantDTO fromParticipant(Participant participant) {
        return new ParticipantDTO(
                participant.getId(),
                participant.getUserId(),
                participant.getUsername(),
                participant.getPosition(),
                participant.getRoleIndex()
        );
    }

    public static List<ParticipantDTO> fromParticipants(List<Participant> participants) {
        return participants.stream()
                .map(ParticipantDTO::fromParticipant)
                .toList();
    }
}
