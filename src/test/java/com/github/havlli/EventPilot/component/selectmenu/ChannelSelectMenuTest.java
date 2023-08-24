package com.github.havlli.EventPilot.component.selectmenu;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.channel.TextChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChannelSelectMenuTest {

    private AutoCloseable autoCloseable;
    private ChannelSelectMenu underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);

        TextChannel textChannelMockOne = mock(TextChannel.class);
        when(textChannelMockOne.getName()).thenReturn("textChannelOne");
        when(textChannelMockOne.getId()).thenReturn(Snowflake.of(1));

        TextChannel textChannelMockTwo = mock(TextChannel.class);
        when(textChannelMockTwo.getName()).thenReturn("textChannelTwo");
        when(textChannelMockTwo.getId()).thenReturn(Snowflake.of(2));

        List<TextChannel> textChannels = List.of(textChannelMockOne, textChannelMockTwo);
        underTest = new ChannelSelectMenu(textChannels);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void getCustomId() {
        // Arrange
        String expected = "destination_channel";

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
            assertThat(selectMenu.getCustomId()).isEqualTo("destination_channel");
            assertThat(selectMenu.getOptions()).hasSize(2);
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
            assertThat(selectMenu.getCustomId()).isEqualTo("destination_channel");
            assertThat(selectMenu.getOptions()).hasSize(2);
            assertThat(selectMenu.isDisabled()).isTrue();
        });
    }
}