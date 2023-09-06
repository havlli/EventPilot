package com.github.havlli.EventPilot.component.selectmenu;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CustomSelectMenuTest {

    private CustomSelectMenu underTest;
    private static final Map<Integer,String> givenMap = Map.of(
            1, "first",
            2, "second"
    );
    private static final String givenCustomId = "customId";
    private static final String givenPlaceholder = "placeholder";

    @BeforeEach
    void setUp() {
        underTest = new CustomSelectMenu(givenCustomId, givenPlaceholder, givenMap);
    }

    @Test
    void getCustomId() {
        // Act
        String customId = underTest.getCustomId();

        // Assert
        assertThat(customId).isEqualTo(givenCustomId);
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
            assertThat(selectMenu.getCustomId()).isEqualTo(givenCustomId);
            assertThat(selectMenu.getPlaceholder()).hasValue(givenPlaceholder);
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
            assertThat(selectMenu.getCustomId()).isEqualTo(givenCustomId);
            assertThat(selectMenu.getPlaceholder()).hasValue(givenPlaceholder);
            assertThat(selectMenu.isDisabled()).isTrue();
        });
    }
}