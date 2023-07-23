package com.github.havlli.EventPilot.component;

import discord4j.core.object.component.ActionRow;

import java.util.List;

public interface ButtonRowComponent {
    List<String> getCustomIds();
    ActionRow getActionRow();
    ActionRow getDisabledRow();
}
