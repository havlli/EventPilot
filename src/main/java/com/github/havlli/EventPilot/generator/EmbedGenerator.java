package com.github.havlli.EventPilot.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.participant.Participant;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class EmbedGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(EmbedGenerator.class);
    private final EmbedFormatter formatter;
    private final ComponentGenerator generator;
    private final EmbedTypeService embedTypeService;
    private final EmbedInteractionGenerator interactionGenerator;

    private final static String DELIMITER = ",";

    public EmbedGenerator(
            EmbedFormatter formatter,
            ComponentGenerator generator,
            EmbedTypeService embedTypeService,
            EmbedInteractionGenerator interactionGenerator
    ) {
        this.formatter = formatter;
        this.generator = generator;
        this.embedTypeService = embedTypeService;
        this.interactionGenerator = interactionGenerator;
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
        String raidSize = formatter.raidSize(event.getParticipants().size(), event.getMemberSize());
        String date = formatter.date(event.getDateTime());
        String time = formatter.time(event.getDateTime());

        return EmbedCreateSpec.builder()
                .addField(empty, leaderWithEmbedId, false)
                .addField(event.getName(), empty, false)
                .addField(empty, event.getDescription(), false)
                .addField(empty, date, true)
                .addField(empty, time, true)
                .addField(empty, raidSize, true)
                .addAllFields(constructPopulatedFields(event))
                .build();
    }

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

    public List<EmbedCreateFields.Field> constructPopulatedFields(Event event) {
        EmbedType embedType = event.getEmbedType();
        List<Participant> eventParticipants = event.getParticipants();

        try {
            return embedTypeService.getDeserializedMap(embedType)
                    .entrySet()
                    .stream()
                    .sorted(sortFieldKeys())
                    .filter(entry -> optimizedFilter(eventParticipants).test(entry))
                    .map(mapEntryToField(eventParticipants))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            LOG.error("Serialization error - {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    private static Comparator<Map.Entry<Integer, String>> sortFieldKeys() {
        return (entry1, entry2) -> {
            int value1 = entry1.getKey();
            int value2 = entry2.getKey();

            if (value1 > 0 && value2 > 0) {
                return Integer.compare(value1, value2);
            } else if (value1 <= 0 && value2 <= 0) {
                return Integer.compare(value1, value2);
            } else if (value1 > 0) {
                return -1;
            } else {
                return 1;
            }
        };
    }

    private EmbedCreateFields.Field createEmbedField(String content, boolean isOneLineField) {
        if (isOneLineField)
            return EmbedCreateFields.Field.of("", content, false);
        else
            return EmbedCreateFields.Field.of(content, "", true);
    }

    public List<LayoutComponent> generateComponents(Event event) {
        return generator.eventButtons(DELIMITER, event);
    }

    public void subscribeInteractions(Event event) {
        interactionGenerator.subscribeInteractions(event, this);
    }

    public String getDelimiter() {
        return DELIMITER;
    }
}
