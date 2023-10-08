package com.github.havlli.EventPilot.component;

import com.github.havlli.EventPilot.component.selectmenu.*;
import discord4j.core.object.entity.channel.TextChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomComponentFactoryTest {

    private CustomComponentFactory underTest;
    @Mock
    private List<TextChannel> textChannelsMock;

    @BeforeEach
    void setUp() {
        underTest = new CustomComponentFactory();
    }

    @Test
    void getDefaultSelectMenu_ReturnsRaidSelectMenu_WhenTypeIsRaidSelectMenu() {
        // Arrange
        CustomComponentFactory.SelectMenuType givenType = CustomComponentFactory.SelectMenuType.RAID_SELECT_MENU;
        Class<? extends SelectMenuComponent> expectedClass = RaidSelectMenu.class;

        // Act
        SelectMenuComponent actual = underTest.getDefaultSelectMenu(givenType);

        // Assert
        assertThat(actual).isInstanceOf(expectedClass);
    }

    @Test
    void getDefaultSelectMenu_ReturnsExpiredSelectMenu_WhenTypeIsExpiredSelectMenu() {
        // Arrange
        CustomComponentFactory.SelectMenuType givenType = CustomComponentFactory.SelectMenuType.EXPIRED_SELECT_MENU;
        Class<? extends SelectMenuComponent> expectedClass = ExpiredSelectMenu.class;

        // Act
        SelectMenuComponent actual = underTest.getDefaultSelectMenu(givenType);

        // Assert
        assertThat(actual).isInstanceOf(expectedClass);
    }

    @Test
    void getDefaultSelectMenu_ReturnsMemberSizeSelectMenu_WhenTypeIsMemberSizeSelectMenu() {
        // Arrange
        CustomComponentFactory.SelectMenuType givenType = CustomComponentFactory.SelectMenuType.MEMBER_SIZE_SELECT_MENU;
        Class<? extends SelectMenuComponent> expectedClass = MemberSizeSelectMenu.class;

        // Act
        SelectMenuComponent actual = underTest.getDefaultSelectMenu(givenType);

        // Assert
        assertThat(actual).isInstanceOf(expectedClass);
    }

    @Test
    void getChannelSelectMenu() {
        // Arrange
        Class<? extends SelectMenuComponent> expectedClass = ChannelSelectMenu.class;

        // Act
        SelectMenuComponent channelSelectMenu = underTest.getChannelSelectMenu(textChannelsMock);

        // Assert
        assertThat(channelSelectMenu).isInstanceOf(expectedClass);
    }

    @Test
    void getCustomSelectMenu() {
        // Arrange
        String customId = "1234";
        String placeholder = "test";
        Map<Long, String> options = Map.of(1L, "test");
        Class<? extends SelectMenuComponent> expectedClass = CustomSelectMenu.class;

        // Act
        SelectMenuComponent actual = underTest.getCustomSelectMenu(customId, placeholder, options);

        // Assert
        assertThat(actual).isInstanceOf(expectedClass);
    }

    @Test
    void getConfirmationButtonRow() {
        // Act
        ButtonRowComponent actual = underTest.getConfirmationButtonRow();

        // Assert
        assertThat(actual).isInstanceOf(ButtonRow.class);
    }
}