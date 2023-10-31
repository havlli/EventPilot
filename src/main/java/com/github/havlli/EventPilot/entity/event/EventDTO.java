package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeDTO;
import com.github.havlli.EventPilot.entity.guild.GuildDTO;
import com.github.havlli.EventPilot.entity.participant.ParticipantDTO;

import java.time.Instant;
import java.util.List;

public record EventDTO(
        String eventId,
        String name,
        String description,
        String author,
        Instant dateTime,
        String destinationChannelId,
        String memberSize,
        List<ParticipantDTO> participants,
        GuildDTO guild,
        EmbedTypeDTO embedType
) {
    public static EventDTO fromEvent(Event event) {
        return new EventDTO(
                event.getEventId(),
                event.getName(),
                event.getDescription(),
                event.getAuthor(),
                event.getDateTime(),
                event.getDestinationChannelId(),
                event.getMemberSize(),
                ParticipantDTO.fromParticipants(event.getParticipants()),
                GuildDTO.fromGuild(event.getGuild()),
                EmbedTypeDTO.fromEmbedType(event.getEmbedType())
        );
    }
}
