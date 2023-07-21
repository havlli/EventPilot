package com.github.havlli.EventPilot.component.selectmenu;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.channel.TextChannel;

import java.util.ArrayList;
import java.util.List;

public class ChannelSelectMenu implements SelectMenuComponent {
    private static final String CUSTOM_ID = "destination_channel";
    private static final String PLACEHOLDER = "Choose channel to post signup in!";
    private final List<TextChannel> textChannels;

    public ChannelSelectMenu(List<TextChannel> textChannels) {
        this.textChannels = textChannels;
    }

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ActionRow getActionRow() {
        SelectMenu selectMenu = SelectMenu.of(CUSTOM_ID, buildOptions())
                .withPlaceholder(PLACEHOLDER)
                .withMaxValues(1)
                .withMinValues(1);

        return ActionRow.of(selectMenu);
    }

    private List<SelectMenu.Option> buildOptions() {
        List<SelectMenu.Option> selectOptions = new ArrayList<>();
        for (TextChannel textChannel : textChannels) {
            selectOptions.add(SelectMenu.Option.of(textChannel.getName(), textChannel.getId().asString()));
        }

        return selectOptions;
    }
}
