package com.github.havlli.EventPilot.component;

import discord4j.core.object.component.ActionRow;

public interface SelectMenuComponent {
    String getCustomId();
    ActionRow getActionRow();
    ActionRow getDisabledRow();
}
