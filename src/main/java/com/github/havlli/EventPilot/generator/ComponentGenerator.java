package com.github.havlli.EventPilot.generator;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ComponentGenerator {

    public List<LayoutComponent> eventButtons(String delimiter, String id, Map<Integer, String> roles) {
        return Arrays.asList(
                ActionRow.of(generateRoleButtons(delimiter, id, roles)),
                ActionRow.of(generateDefaultButtons(delimiter, id, roles))
        );
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
