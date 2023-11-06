package com.github.havlli.EventPilot.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionReplyEditSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class EmbedInteractionGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(EmbedInteractionGenerator.class);
    private final EmbedTypeService embedTypeService;
    private final GatewayDiscordClient client;
    private final ParticipantService participantService;
    private final EventService eventService;

    public EmbedInteractionGenerator(
            EmbedTypeService embedTypeService,
            GatewayDiscordClient client,
            ParticipantService participantService,
            EventService eventService
    ) {
        this.embedTypeService = embedTypeService;
        this.client = client;
        this.participantService = participantService;
        this.eventService = eventService;
    }

    public void subscribeInteractions(Event event, EmbedGenerator embedGenerator) {
        EmbedType embedType = event.getEmbedType();
        String delimiter = embedGenerator.getDelimiter();

        try {
            embedTypeService.getDeserializedMap(embedType)
                    .forEach((key, value) -> {
                        String customId = constructCustomId(event.getEventId(), delimiter, key);
                        client.getEventDispatcher().on(ButtonInteractionEvent.class)
                                .filter(interaction -> interaction.getCustomId().equals(customId))
                                .flatMap(interaction -> handleEvent(interaction, event, embedGenerator))
                                .subscribe();
                        LOG.info("Interaction %s for Event %s got subscribed".formatted(value, event.getEventId()));
                    });
        } catch (JsonProcessingException e) {
            LOG.error("Serialization error - %s".formatted(e.getMessage()));
        }
    }

    public Mono<Message> handleEvent(ButtonInteractionEvent event, Event embedEvent, EmbedGenerator embedGenerator) {
        List<Participant> participants = embedEvent.getParticipants();
        User user = event.getInteraction().getUser();
        String userId = user.getId().asString();
        int roleIndex = extractRoleIndex(event.getCustomId(), embedGenerator.getDelimiter());

        Optional<Participant> participant = participantService.getParticipant(userId, participants);
        if (participant.isEmpty()) {
            Integer currentOrder = participants.size() + 1;
            Participant newParticipant = new Participant(userId, user.getUsername(), currentOrder, roleIndex, embedEvent);
            participantService.addParticipant(newParticipant, participants);
        } else {
            participantService.updateRoleIndex(participant.get(), roleIndex);
        }

        eventService.saveEvent(embedEvent);

        return event.deferEdit()
                .then(event.editReply(InteractionReplyEditSpec.builder()
                        .addEmbed(embedGenerator.generateEmbed(embedEvent))
                        .build())
                );
    }

    private int extractRoleIndex(String customId, String delimiter) {
        return Integer.parseInt(customId.split(delimiter)[1]);
    }

    private String constructCustomId(String id, String delimiter, Integer key) {
        return id + delimiter + key;
    }
}
