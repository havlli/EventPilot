package com.github.havlli.EventPilot.prompt;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.json.ChannelData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptServiceTest {

    private AutoCloseable autoCloseable;
    private PromptService underTest;
    @Mock
    private GatewayDiscordClient mockClient;
    
    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new PromptService(mockClient);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void fetchGuildTextChannels_ReturnsFilteredFluxOfTextChannels_WhenMultipleTypesArePresent() {
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
        Flux<TextChannel> resultFlux = underTest.fetchGuildTextChannels(event);

        // Assert
        StepVerifier.create(resultFlux)
                .expectNext(textChannel1)
                .expectNext(textChannel2)
                .verifyComplete();
        List<TextChannel> actualList = resultFlux.collectList().block();
        assertThat(actualList).hasSize(2);
    }

    @Test
    public void fetchGuildTextChannels_ExcludesObfuscatedChannelsAndKeepsNormalChannels() {
        Snowflake guildId = Snowflake.of("123456789");
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn(guildId);
        InteractionCreateEvent event = mock(InteractionCreateEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getGuild()).thenReturn(Mono.just(guild));

        // Exercise Discord4J's real flag parsing and isObfuscated(), without a gateway connection.
        assertThat(Channel.Flag.CHANNEL_OBFUSCATED.getBitValue()).isEqualTo(1 << 17);
        TextChannel normal = new TextChannel(mockClient, ChannelData.builder()
                .id(1L).type(0).guildId(guildId.asLong()).name("events").build());
        TextChannel hidden = new TextChannel(mockClient, ChannelData.builder()
                .id(2L).type(0).guildId(guildId.asLong()).name("___hidden___")
                .flags((1 << 17) | Channel.Flag.PINNED.getBitValue()).build());
        // Names alone must not determine visibility.
        TextChannel visibleWithHiddenName = new TextChannel(mockClient, ChannelData.builder()
                .id(3L).type(0).guildId(guildId.asLong()).name("___hidden___").flags(0).build());
        TextChannel visibleWithOtherFlag = new TextChannel(mockClient, ChannelData.builder()
                .id(4L).type(0).guildId(guildId.asLong()).name("other")
                .flags(Channel.Flag.PINNED.getBitValue()).build());
        when(mockClient.getGuildChannels(guildId)).thenReturn(
                Flux.just(normal, hidden, visibleWithHiddenName, visibleWithOtherFlag));

        StepVerifier.create(underTest.fetchGuildTextChannels(event))
                .expectNext(normal, visibleWithHiddenName, visibleWithOtherFlag)
                .verifyComplete();
    }

    @Test
    public void fetchGuildTextChannels_ReturnsEmptyFlux_WhenNoTextChannelsArePresent() {
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
        Flux<TextChannel> resultFlux = underTest.fetchGuildTextChannels(event);

        // Assert
        StepVerifier.create(resultFlux)
                .verifyComplete();
        List<TextChannel> actualList = resultFlux.collectList().block();
        assertThat(actualList).isEmpty();
    }

    @Test
    public void fetchGuildId_ReturnsCorrectSnowflake_WhenGuildIsPresent() {
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
    public void fetchGuildId_ReturnsSnowflakeZero_WhenGuildIsNotPresent() {
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
