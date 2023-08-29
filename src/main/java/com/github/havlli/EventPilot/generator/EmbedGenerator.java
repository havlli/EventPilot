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
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class EmbedGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(EmbedGenerator.class);
    private final GatewayDiscordClient client;
    private final EmbedFormatter formatter;
    private final ComponentGenerator generator;
    private final ParticipantService participantService;
    private final EventService eventService;
    private final EmbedTypeService embedTypeService;

    private final static String DELIMITER = ",";

    public EmbedGenerator(
            GatewayDiscordClient client,
            EmbedFormatter formatter,
            ComponentGenerator generator,
            ParticipantService participantService,
            EventService eventService,
            EmbedTypeService embedTypeService
    ) {
        this.client = client;
        this.formatter = formatter;
        this.generator = generator;
        this.participantService = participantService;
        this.eventService = eventService;
        this.embedTypeService = embedTypeService;
    }

    public EmbedCreateSpec generatePreview(EmbedPreviewable embedPreviewable) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        embedPreviewable.previewFields().forEach((key, value) -> {
            if (!value.isBlank()) {
                String name = key.substring(0,1).toUpperCase().concat(key.substring(1));
                fields.add(EmbedCreateFields.Field.of(name, value, false));
            }
        });

        return EmbedCreateSpec.builder()
                .addAllFields(fields)
                .build();
    }

    public EmbedCreateSpec generateEmbed(Event event) {
        String empty = "";
        String leaderWithEmbedId = formatter.leaderWithId(event.getAuthor(), event.getEventId());
        String raidSize = formatter.raidSize(event.getParticipants().size(), Integer.parseInt(event.getMemberSize()));
        String date = formatter.date(event.getDateTime());
        String time = formatter.time(event.getDateTime());

        return EmbedCreateSpec.builder()
                .addField(empty, leaderWithEmbedId, false)
                .addField(event.getName(), empty, false)
                .addField(empty, event.getDescription(), false)
                .addField(empty, date, true)
                .addField(empty, time, true)
                .addField(empty, raidSize, true)
                .addAllFields(getPopulatedFields(event))
                .build();
    }

    private static final HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
            -1, "Absence",
            -2, "Late",
            -3, "Tentative",
            1, "Tank",
            2, "Melee",
            3, "Ranged",
            4, "Healer",
            5, "Support"
    ));

    private List<Participant> getMatchingUsers(int roleIndex, List<Participant> participants) {
        return participants.stream()
                .filter(participant -> participant.getRoleIndex() == roleIndex)
                .collect(Collectors.toList());
    }

    private Predicate<Integer> isOneLineField() {
        return integer -> integer < 0;
    }

    private Predicate<Map.Entry<Integer, String>> optimizedFilter(List<Participant> participants) {
        return entry -> {
            for (Participant participant : participants) {
                if (participant.getRoleIndex().equals(entry.getKey())) return true;
            }
            return false;
        };
    }

    private Function<Map.Entry<Integer, String>, EmbedCreateFields.Field> mapEntryToField(List<Participant> participants) {
        return entry -> {
            int index = entry.getKey();
            String name = entry.getValue();
            boolean isOneLineField = isOneLineField().test(index);
            List<Participant> matchingUsers = getMatchingUsers(index, participants);
            String fieldConcat = formatter.createConcatField(name, matchingUsers, isOneLineField);

            return createEmbedField(fieldConcat, isOneLineField);
        };
    }

    public List<EmbedCreateFields.Field> getPopulatedFields(Event event) {
        EmbedType embedType = event.getEmbedType();
        List<Participant> eventParticipants = event.getParticipants();

        try {
            HashMap<Integer, String> deserializedMap = embedTypeService.getDeserializedMap(embedType);
            return deserializedMap.entrySet()
                    .stream()
                    .filter(entry -> optimizedFilter(eventParticipants).test(entry))
                    .map(mapEntryToField(eventParticipants))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            LOG.error("Serialization error - {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    private EmbedCreateFields.Field createEmbedField(String content, boolean isOneLineField) {
        if (isOneLineField)
            return EmbedCreateFields.Field.of("", content, false);
        else
            return EmbedCreateFields.Field.of(content, "", true);
    }

    public List<LayoutComponent> generateComponents(String id) {
        return generator.eventButtons(DELIMITER, id, fieldsMap);
    }

    public Mono<Message> handleEvent(ButtonInteractionEvent event, Event embedEvent) {
        List<Participant> participants = embedEvent.getParticipants();
        User user = event.getInteraction().getUser();
        String userId = user.getId().asString();
        int roleIndex = extractRoleIndex(event.getCustomId());

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
                        .addEmbed(this.generateEmbed(embedEvent))
                        .build())
                );
    }

    private int extractRoleIndex(String customId) {
        return Integer.parseInt(customId.split(DELIMITER)[1]);
    }

    public void subscribeInteractions(Event event) {
        fieldsMap.forEach((fieldKey, value) -> {
            String customId = event.getEventId() + DELIMITER + fieldKey;
            client.getEventDispatcher().on(ButtonInteractionEvent.class)
                    .filter(interaction -> interaction.getCustomId().equals(customId))
                    .flatMap(interaction -> handleEvent(interaction, event))
                    .subscribe();
        });
    }
}
