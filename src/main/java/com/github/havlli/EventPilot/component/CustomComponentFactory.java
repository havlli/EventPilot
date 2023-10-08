package com.github.havlli.EventPilot.component;

import com.github.havlli.EventPilot.component.selectmenu.*;
import discord4j.core.object.entity.channel.TextChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CustomComponentFactory {
    public enum SelectMenuType {
        EXPIRED_SELECT_MENU,
        MEMBER_SIZE_SELECT_MENU,
        RAID_SELECT_MENU
    }

    public SelectMenuComponent getDefaultSelectMenu(SelectMenuType type) {
        return switch (type) {
            case RAID_SELECT_MENU -> new RaidSelectMenu();
            case EXPIRED_SELECT_MENU -> new ExpiredSelectMenu();
            case MEMBER_SIZE_SELECT_MENU -> new MemberSizeSelectMenu();
        };
    }

    public SelectMenuComponent getChannelSelectMenu(List<TextChannel> textChannels) {
        return new ChannelSelectMenu(textChannels);
    }

    public SelectMenuComponent getCustomSelectMenu(String customId, String placeholder, Map<Long, String> options) {
        return new CustomSelectMenu(customId, placeholder, options);
    }

    public ButtonRowComponent getConfirmationButtonRow() {
        return ButtonRow.builder()
                .addButton("confirm", "Confirm", ButtonRow.Builder.ButtonType.PRIMARY)
                .addButton("cancel", "Cancel", ButtonRow.Builder.ButtonType.DANGER)
                .addButton("repeat", "Start Again!", ButtonRow.Builder.ButtonType.SECONDARY)
                .build();
    }
}
