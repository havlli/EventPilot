package com.github.havlli.EventPilot.generator;

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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class EmbedGenerator {

    private final GatewayDiscordClient client;
    private final EmbedFormatter formatter;
    private final ComponentGenerator generator;
    private final ParticipantService participantService;
    private final EventService eventService;

    private final static String DELIMITER = ",";

    public EmbedGenerator(
            GatewayDiscordClient client,
            EmbedFormatter formatter,
            ComponentGenerator generator,
            ParticipantService participantService,
            EventService eventService
    ) {
        this.client = client;
        this.formatter = formatter;
        this.generator = generator;
        this.participantService = participantService;
        this.eventService = eventService;
    }

    public EmbedCreateSpec generatePreview(EmbedPreviewable embedPreviewable) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        embedPreviewable.previewFields().forEach((key, value) -> {
            String name = key.substring(0,1).toUpperCase().concat(key.substring(1));

            if (value != null)
                fields.add(EmbedCreateFields.Field.of(name, value, false));
        });

        return EmbedCreateSpec.builder()
                .addAllFields(fields)
                .build();
    }

    public EmbedCreateSpec generateEmbed(Event event) {
        String empty = "";
        String leaderWithEmbedId = formatter.leaderWithId(event.getAuthor(), event.getEventId());
        String raidSize = formatter.raidSize(event.getParticipants().size(), Integer.parseInt(event.getMemberSize()));
        long timestamp = getTimestamp(event.getDateTime());
        String date = formatter.date(timestamp);
        String time = formatter.time(timestamp);

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

    private Long getTimestamp(Instant dateTime) {
        return dateTime.getEpochSecond();
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

    private String buildFieldConcat(String fieldName, List<Participant> matchingUsers, boolean isOneLineField) {
        int count = matchingUsers.size();
        String lineSeparator = isOneLineField ? ", " : "\n";

        return String.format("%s (%d):%s%s",
                fieldName,
                count,
                isOneLineField ? " " : "\n",
                matchingUsers.stream()
                        .map(participant -> String.format("`%d`%s", participant.getPosition(), participant.getUsername()))
                        .collect(Collectors.joining(lineSeparator))
        );
    }

    public List<EmbedCreateFields.Field> getPopulatedFields(Event event) {
        List<EmbedCreateFields.Field> populatedFields = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : fieldsMap.entrySet()) {
            int fieldIndex = entry.getKey();
            String fieldName = entry.getValue();

            List<Participant> matchingUsers = getMatchingUsers(fieldIndex, event.getParticipants());
            if (!matchingUsers.isEmpty()) {
                boolean isOneLineField = fieldIndex < 0;
                String fieldConcat = buildFieldConcat(fieldName, matchingUsers, isOneLineField);

                if (isOneLineField) {
                    populatedFields.add(EmbedCreateFields.Field.of("", fieldConcat, false));
                } else {
                    populatedFields.add(EmbedCreateFields.Field.of(fieldConcat, "", true));
                }
            }
        }

        return populatedFields;
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
            participantService.updateParticipant(participant.get(), roleIndex);
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
