package com.github.havlli.EventPilot.component.selectmenu;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;

import java.util.ArrayList;
import java.util.List;

public class RaidSelectMenu implements SelectMenuComponent {

    private static final String CUSTOM_ID = "raid_select";
    private static final String PLACEHOLDER = "Choose Raids for this event!";

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ActionRow getActionRow() {
        SelectMenu selectMenu = SelectMenu.of(CUSTOM_ID, buildOptions())
                .withPlaceholder(PLACEHOLDER)
                .withMaxValues(3)
                .withMinValues(1);

        return ActionRow.of(selectMenu);
    }

    private List<SelectMenu.Option> buildOptions() {
        List<SelectMenu.Option> selectOptions = new ArrayList<>();
        selectOptions.add(SelectMenu.Option.of("Molten Core - Normal", "Molten Core - Normal"));
        selectOptions.add(SelectMenu.Option.of("Molten Core - Heroic", "Molten Core - Heroic"));
        selectOptions.add(SelectMenu.Option.of("Molten Core - Mythic", "Molten Core - Mythic"));
        selectOptions.add(SelectMenu.Option.of("Molten Core - Ascended", "Molten Core - Ascended"));
        selectOptions.add(SelectMenu.Option.of("Onyxia - Normal", "Onyxia - Normal"));

        return selectOptions;
    }
}
