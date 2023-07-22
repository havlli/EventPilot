package com.github.havlli.EventPilot.component.selectmenu;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;

import java.util.ArrayList;
import java.util.List;

public class MemberSizeSelectMenu implements SelectMenuComponent {
    private static final String CUSTOM_ID = "member_size";
    private static final String PLACEHOLDER = "Choose maximum attendants!";

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
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
        return SelectMenu.of(CUSTOM_ID, buildOptions())
                .withPlaceholder(PLACEHOLDER)
                .withMaxValues(1)
                .withMinValues(1);
    }

    private List<SelectMenu.Option> buildOptions() {
        List<SelectMenu.Option> selectOptions = new ArrayList<>();
        selectOptions.add(SelectMenu.Option.of("10", "10"));
        selectOptions.add(SelectMenu.Option.of("15", "15"));
        selectOptions.add(SelectMenu.Option.of("20", "20"));
        selectOptions.add(SelectMenu.Option.of("25", "25"));

        return selectOptions;
    }
}
