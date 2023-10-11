package com.github.havlli.EventPilot.entity.event;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.participant.Participant;

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
        List<Participant> participants,
        Guild guild,
        EmbedType embedType
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
                event.getParticipants(),
                new Guild(event.getGuild().getId(), event.getGuild().getName()),
                EmbedType.builder()
                        .withName(event.getEmbedType().getName())
                        .withStructure(event.getEmbedType().getStructure())
                        .build()
        );
    }
}
