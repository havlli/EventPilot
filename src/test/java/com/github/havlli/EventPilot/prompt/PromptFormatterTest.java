package com.github.havlli.EventPilot.prompt;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptFormatterTest {

    private AutoCloseable autoCloseable;
    private PromptFormatter underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new PromptFormatter();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void formatResponse_WillReturnJoinedString_WhenEventIsPassed() {
        // Arrange
        SelectMenuInteractionEvent eventMock = mock(SelectMenuInteractionEvent.class);
        List<String> eventValues = List.of("value1","value2","value3");
        when(eventMock.getValues()).thenReturn(eventValues);
        String expected = "value1, value2, value3";

        // Act
        String actual = underTest.formatResponse(eventMock);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void formatResponse_WillThrow_WhenNullObjectIsPassed() {
        // Arrange
        SelectMenuInteractionEvent event = null;

        // Assert
        assertThatThrownBy(() -> underTest.formatResponse(event))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void messageUrl_WillReturnString() {
        // Arrange
        Snowflake guildIdMock = mock(Snowflake.class);
        Snowflake channelIdMock = mock(Snowflake.class);
        Snowflake messageIdMock = mock(Snowflake.class);
        when(guildIdMock.asString()).thenReturn("5");
        when(channelIdMock.asString()).thenReturn("5");
        when(messageIdMock.asString()).thenReturn("5");

        String expected = "https://discord.com/channels/5/5/5";

        // Act
        String actual = underTest.messageUrl(guildIdMock, channelIdMock, messageIdMock);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void messageUrl_WillThrow_WhenOneOrMoreParametersAreNull() {
        // Assert
        assertThatThrownBy(() -> underTest.messageUrl(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void channelUrl_WillReturnString() {
        // Arrange
        Snowflake guildIdMock = mock(Snowflake.class);
        Snowflake channelIdMock = mock(Snowflake.class);
        when(guildIdMock.asString()).thenReturn("5");
        when(channelIdMock.asString()).thenReturn("5");

        String expected = "https://discord.com/channels/5/5";

        // Act
        String actual = underTest.channelUrl(guildIdMock, channelIdMock);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void channelUrl_WillThrow_WhenOneOrMoreParametersAreNull() {
        // Assert
        assertThatThrownBy(() -> underTest.channelUrl(null, null))
                .isInstanceOf(NullPointerException.class);
    }
}