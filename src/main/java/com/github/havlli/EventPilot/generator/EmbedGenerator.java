package com.github.havlli.EventPilot.generator;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.participant.Participant;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmbedGenerator {

    private final EmbedFormatter formatter;
    private final ComponentGenerator generator;

    public EmbedGenerator(EmbedFormatter formatter, ComponentGenerator generator) {
        this.formatter = formatter;
        this.generator = generator;
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

    private Long getTimestamp(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).getEpochSecond();
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
        return generator.eventButtons(",", id, fieldsMap);
    }
}
