package com.github.havlli.EventPilot.component.selectmenu;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RaidSelectMenuTest {

    private RaidSelectMenu underTest;

    @BeforeEach
    void setUp() {
        underTest = new RaidSelectMenu();
    }

    @Test
    void getCustomId() {
        // Arrange
        String expected = "raid_select";

        // Act
        String actual = underTest.getCustomId();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getActionRow() {
        // Act
        ActionRow actual = underTest.getActionRow();

        // Assert
        Optional<SelectMenu> actualSelectMenu = actual.getChildren().stream()
                .filter(c -> c instanceof SelectMenu)
                .map(c -> ((SelectMenu) c))
                .findFirst();

        assertThat(actualSelectMenu).hasValueSatisfying(selectMenu -> {
            assertThat(selectMenu.getOptions()).hasSize(5);
            assertThat(selectMenu.getPlaceholder()).hasValue("Choose Raids for this event!");
            assertThat(selectMenu.isDisabled()).isFalse();
        });
    }

    @Test
    void getDisabledRow() {
        // Act
        ActionRow actual = underTest.getDisabledRow();

        // Assert
        Optional<SelectMenu> actualSelectMenu = actual.getChildren().stream()
                .filter(c -> c instanceof SelectMenu)
                .map(c -> ((SelectMenu) c))
                .findFirst();

        assertThat(actualSelectMenu).hasValueSatisfying(selectMenu -> {
            assertThat(selectMenu.getOptions()).hasSize(5);
            assertThat(selectMenu.getPlaceholder()).hasValue("Choose Raids for this event!");
            assertThat(selectMenu.isDisabled()).isTrue();
        });
    }
}