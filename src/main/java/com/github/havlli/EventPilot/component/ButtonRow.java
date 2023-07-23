package com.github.havlli.EventPilot.component;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;

import java.util.ArrayList;
import java.util.List;

public class ButtonRow implements ButtonRowComponent {

    private final List<Button> buttons;
    private final List<String> customIds;

    public ButtonRow(Builder builder) {
        this.buttons = builder.buttons;
        this.customIds = builder.customIds;
    }

    @Override
    public List<String> getCustomIds() {
        return customIds;
    }

    @Override
    public ActionRow getActionRow() {
        return ActionRow.of(buttons);
    }

    @Override
    public ActionRow getDisabledRow() {
        List<Button> disabledButtons = buttons.stream()
                .map(button -> button.disabled(true))
                .toList();
        return ActionRow.of(disabledButtons);
    }

    public static ButtonRow.Builder builder() {
        return new ButtonRow.Builder();
    }

    public static class Builder {
        public enum buttonType {
            PRIMARY,
            SECONDARY,
            DANGER
        }

        private final List<Button> buttons;
        private final List<String> customIds;

        Builder() {
            this.buttons = new ArrayList<>();
            this.customIds = new ArrayList<>();
        }

        public Builder addButton(String customId, String label, buttonType type) {

            switch (type) {
                case PRIMARY -> {
                    buttons.add(Button.primary(customId, label));
                    customIds.add(customId);
                }
                case SECONDARY -> {
                    buttons.add(Button.secondary(customId, label));
                    customIds.add(customId);
                }
                case DANGER -> {
                    buttons.add(Button.danger(customId, label));
                    customIds.add(customId);
                }
            }

            return this;
        }

        public ButtonRow build() {
            return new ButtonRow(this);
        }

    }
}
