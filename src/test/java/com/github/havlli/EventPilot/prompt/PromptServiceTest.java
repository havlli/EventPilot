package com.github.havlli.EventPilot.prompt;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptServiceTest {

    private AutoCloseable autoCloseable;
    @InjectMocks
    private PromptService underTest;
    @Mock
    private GatewayDiscordClient mockClient;
    
    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void fetchGuildTextChannels_WillFilterTextChannels_WhenMultipleTypesArePresent() {
        // Arrange
        TextChannel textChannel1 = mock(TextChannel.class);
        TextChannel textChannel2 = mock(TextChannel.class);
        VoiceChannel voiceChannel = mock(VoiceChannel.class);
        List<GuildChannel> channels = List.of(textChannel1, textChannel2, voiceChannel);
        Snowflake guildId = Snowflake.of("123456789");
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(guildId);

        InteractionCreateEvent event = mock(InteractionCreateEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getGuild()).thenReturn(Mono.just(guild));
        when(mockClient.getGuildChannels(guildId)).thenReturn(Flux.fromIterable(channels));

        // Act
        List<TextChannel> resultChannels = underTest.fetchGuildTextChannels(event);

        // Assert
        assertThat(resultChannels.size()).isEqualTo(2);
        assertThat(resultChannels).contains(textChannel1, textChannel2);
    }

    @Test
    void fetchGuildTextChannels_WillReturnEmptyList_WhenNoTextChannelsArePresent() {
        // Arrange
        VoiceChannel voiceChannel1 = mock(VoiceChannel.class);
        VoiceChannel voiceChannel2 = mock(VoiceChannel.class);
        List<GuildChannel> channels = List.of(voiceChannel1, voiceChannel2);
        Snowflake guildId = Snowflake.of("123456789");
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(guildId);

        InteractionCreateEvent event = mock(InteractionCreateEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getGuild()).thenReturn(Mono.just(guild));
        when(mockClient.getGuildChannels(guildId)).thenReturn(Flux.fromIterable(channels));

        // Act
        List<TextChannel> resultChannels = underTest.fetchGuildTextChannels(event);

        // Assert
        assertThat(resultChannels.size()).isEqualTo(0);
    }

    @Test
    void fetchGuildId_WillReturnCorrectSnowflake_WhenGuildIsPresent() {
        // Arrange
        Snowflake guildId = Snowflake.of("123456789");
        InteractionCreateEvent event = mock(InteractionCreateEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.of(guildId));

        // Act
        Snowflake resultGuildId = underTest.fetchGuildId(event);

        // Assert
        assertThat(resultGuildId).isEqualTo(guildId);
    }

    @Test
    void fetchGuildId_WillReturnSnowflakeZero_WhenGuildIsNotPresent() {
        // Arrange
        InteractionCreateEvent event = mock(InteractionCreateEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.empty());

        // Act
        Snowflake resultGuildId = underTest.fetchGuildId(event);

        // Assert
        Snowflake expected = Snowflake.of(0);
        assertThat(resultGuildId).isEqualTo(expected);
    }
}