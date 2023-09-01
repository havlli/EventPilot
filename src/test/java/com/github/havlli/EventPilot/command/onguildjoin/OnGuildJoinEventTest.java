package com.github.havlli.EventPilot.command.onguildjoin;

import com.github.havlli.EventPilot.entity.guild.GuildService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.Guild;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OnGuildJoinEventTest {

    private OnGuildJoinEvent underTest;

    private AutoCloseable autoCloseable;
    @Mock
    private GuildService guildServiceMock;
    @Mock
    private GuildCreateEvent guildCreateEventMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new OnGuildJoinEvent(guildServiceMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle() {
        // Arrange
        Guild guildMock = mock(Guild.class);
        when(guildCreateEventMock.getGuild()).thenReturn(guildMock);
        Snowflake guildId = Snowflake.of(123);
        String guildName = "guild-name";
        when(guildMock.getId()).thenReturn(guildId);
        when(guildMock.getName()).thenReturn(guildName);

        // Act
        Mono<?> actualMono = underTest.handle(guildCreateEventMock);

        // Assert
        verify(guildServiceMock, times(1))
                .createGuildIfNotExists(guildId.asString(), guildName);
        StepVerifier.create(actualMono)
                .expectComplete()
                .verify();
    }

    @Test
    void getName() {
        // Arrange
        String expected = "on-guild-join";

        // Act
        String actual = underTest.getName();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getEventType() {
        // Arrange
        Class<? extends Event> expected = GuildCreateEvent.class;

        // Act
        Class<? extends Event> actual = underTest.getEventType();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void setEventType() {
        // Arrange
        Class<? extends Event> expected = InteractionCreateEvent.class;

        // Act
        underTest.setEventType(expected);

        // Assert
        Class<? extends Event> actual = underTest.getEventType();
        assertThat(actual).isEqualTo(expected);
    }
}