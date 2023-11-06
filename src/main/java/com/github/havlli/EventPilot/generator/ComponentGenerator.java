package com.github.havlli.EventPilot.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeSerialization;
import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ComponentGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentGenerator.class);
    private final EmbedTypeSerialization serialization;

    public ComponentGenerator(EmbedTypeSerialization serialization) {
        this.serialization = serialization;
    }

    public List<LayoutComponent> eventButtons(String delimiter, Event event) {
        try {
            HashMap<Integer, String> roles = serialization.deserializeMap(event.getEmbedType().getStructure());
            return Arrays.asList(
                    ActionRow.of(generateRoleButtons(delimiter, event.getEventId(), roles)),
                    ActionRow.of(generateDefaultButtons(delimiter, event.getEventId(), roles))
            );
        } catch (JsonProcessingException e) {
            LOG.error("Serialization error - %s".formatted(e.getMessage()));
        }

        return new ArrayList<>();
    }

    private List<Button> generateRoleButtons(String delimiter, String id, Map<Integer, String> roles) {
        return roles.entrySet()
                .stream()
                .filter(entry -> entry.getKey() > 0)
                .map(entry -> {
                    String customId = constructCustomId(id, delimiter, entry.getKey());
                    return Button.primary(customId, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    private List<Button> generateDefaultButtons(String delimiter, String id, Map<Integer, String> roles) {
        return roles.entrySet()
                .stream()
                .filter(entry -> entry.getKey() <= 0)
                .map(entry -> {
                    String customId = constructCustomId(id, delimiter, entry.getKey());
                    return Button.secondary(customId, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    private String constructCustomId(String id, String delimiter, Integer fieldKey) {
        return id + delimiter + fieldKey;
    }
}
