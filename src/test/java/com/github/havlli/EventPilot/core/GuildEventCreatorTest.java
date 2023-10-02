package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.prompt.PromptFormatter;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ScheduledEvent;
import discord4j.core.spec.ScheduledEventCreateSpec;
import discord4j.core.spec.ScheduledEventEntityMetadataSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuildEventCreatorTest {

    private AutoCloseable autoCloseable;
    private GuildEventCreator underTest;
    @Mock
    private GatewayDiscordClient clientMock;
    @Mock
    private PromptFormatter formatterMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new GuildEventCreator(clientMock, formatterMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void createScheduledEvent_ReturnsScheduledEventMono() {
        // Arrange
        Guild guild = new Guild("1234", "test-guild");
        Event event = Event.builder()
                .withGuild(guild)
                .withName("test-event")
                .withDescription("Test")
                .withDestinationChannel("1111")
                .withDateTime(Instant.now())
                .withAuthor("test")
                .withEventId("2222")
                .build();

        discord4j.core.object.entity.Guild guildMock = mock(discord4j.core.object.entity.Guild.class);
        when(clientMock.getGuildById(any())).thenReturn(Mono.just(guildMock));
        ScheduledEvent scheduledEventMock = mock(ScheduledEvent.class);
        when(guildMock.createScheduledEvent(any())).thenReturn(Mono.just(scheduledEventMock));
        when(formatterMock.messageUrl(any())).thenReturn("test");

        // Act
        Mono<ScheduledEvent> actual = underTest.createScheduledEvent(event);

        // Assert
        StepVerifier.create(actual)
                .expectNext(scheduledEventMock)
                .verifyComplete();
    }

    @Test
    void scheduledEventCreateSpec_ReturnsScheduledEventCreateSpec() {
        // Arrange
        Guild guild = new Guild("1234", "test-guild");
        Event event = Event.builder()
                .withGuild(guild)
                .withName("test-event")
                .withDescription("Test")
                .withDestinationChannel("1111")
                .withDateTime(Instant.now())
                .withAuthor("test")
                .withEventId("2222")
                .build();

        String location = "test";
        when(formatterMock.messageUrl(event)).thenReturn(location);

        ScheduledEventCreateSpec expected = ScheduledEventCreateSpec
                .builder()
                .privacyLevel(ScheduledEvent.PrivacyLevel.GUILD_ONLY)
                .entityType(ScheduledEvent.EntityType.EXTERNAL)
                .entityMetadata(ScheduledEventEntityMetadataSpec.builder()
                        .location(location)
                        .build())
                .name(event.getName())
                .description(event.getDescription())
                .scheduledStartTime(event.getDateTime())
                .scheduledEndTime(event.getDateTime().plusSeconds(14400))
                .build();

        // Act
        ScheduledEventCreateSpec actual = underTest.scheduledEventCreateSpec(event);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void scheduledEventEntityMetadataSpec_ReturnsScheduledEventEntityMetadataSpec() {
        // Arrange
        Guild guild = new Guild("1234", "test-guild");
        Event event = Event.builder()
                .withGuild(guild)
                .withName("test-event")
                .withDescription("Test")
                .withDestinationChannel("1111")
                .withDateTime(Instant.now())
                .withAuthor("test")
                .withEventId("2222")
                .build();
        String location = "test";
        when(formatterMock.messageUrl(event)).thenReturn(location);

        ScheduledEventEntityMetadataSpec expected = ScheduledEventEntityMetadataSpec.builder()
                .location(location)
                .build();

        // Act
        ScheduledEventEntityMetadataSpec actual = underTest.scheduledEventEntityMetadataSpec(event);

        // Assert
        assertThat(actual).isEqualTo(expected);
        verify(formatterMock, times(1)).messageUrl(event);
    }
}