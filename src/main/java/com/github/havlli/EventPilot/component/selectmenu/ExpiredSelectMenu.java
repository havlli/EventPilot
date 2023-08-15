package com.github.havlli.EventPilot.component.selectmenu;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import org.springframework.stereotype.Component;

@Component
public class ExpiredSelectMenu implements SelectMenuComponent {

    private static final String CUSTOM_ID = "expired";
    private static final String PLACEHOLDER = "Event expired!";

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ActionRow getActionRow() {
        return getDisabledRow();
    }

    @Override
    public ActionRow getDisabledRow() {
        return ActionRow.of(buildSelectMenu().disabled(true));
    }

    private SelectMenu buildSelectMenu() {
        return SelectMenu.of(CUSTOM_ID, SelectMenu.Option.of("ignored", "ignored"))
                .withPlaceholder(PLACEHOLDER);
    }
}
