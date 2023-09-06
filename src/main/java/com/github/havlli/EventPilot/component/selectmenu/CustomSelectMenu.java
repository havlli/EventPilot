package com.github.havlli.EventPilot.component.selectmenu;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomSelectMenu implements SelectMenuComponent {
    private final String customId;
    private final String placeholder;
    private final Map<Integer, String> map;

    public CustomSelectMenu(String customId, String placeholder, Map<Integer, String> map) {
        this.customId = customId;
        this.placeholder = placeholder;
        this.map = map;
    }

    @Override
    public String getCustomId() {
        return customId;
    }

    @Override
    public ActionRow getActionRow() {
        return ActionRow.of(buildSelectMenu());
    }

    @Override
    public ActionRow getDisabledRow() {
        return ActionRow.of(buildSelectMenu().disabled(true));
    }

    private SelectMenu buildSelectMenu() {
        return SelectMenu.of(customId, buildOptions())
                .withPlaceholder(placeholder)
                .withMaxValues(1)
                .withMinValues(1);
    }

    private List<SelectMenu.Option> buildOptions() {
        return map.entrySet().stream()
                .map(entry -> SelectMenu.Option.of(entry.getValue(), entry.getKey().toString()))
                .collect(Collectors.toList());
    }
}
