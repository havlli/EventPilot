package com.github.havlli.EventPilot.component.selectmenu;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiredSelectMenuTest {

    private ExpiredSelectMenu underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExpiredSelectMenu();
    }

    @Test
    void getCustomId() {
        // Arrange
        String expected = "expired";
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
            assertThat(selectMenu.getPlaceholder()).hasValue("Event expired!");
            assertThat(selectMenu.isDisabled()).isTrue();
        });
    }

    @Test
    void getDisabledRow() {
        // Act
        ActionRow actual = underTest.getActionRow();

        // Assert
        Optional<SelectMenu> actualSelectMenu = actual.getChildren().stream()
                .filter(c -> c instanceof SelectMenu)
                .map(c -> ((SelectMenu) c))
                .findFirst();

        assertThat(actualSelectMenu).hasValueSatisfying(selectMenu -> {
            assertThat(selectMenu.getPlaceholder()).hasValue("Event expired!");
            assertThat(selectMenu.isDisabled()).isTrue();
        });
    }
}