package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventInfoCommandTest {

    private AutoCloseable autoCloseable;
    private EventInfoCommand underTest;
    @Mock
    private SimplePermissionValidator permissionChecker;
    @Mock
    private UserSessionValidator userSessionValidator;
    @Mock
    private MessageSource messageSource;
    @Mock
    private EventService eventService;
    @Mock
    private OrganizerEventFormatter organizerEventFormatter;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private GatewayDiscordClient client;
    @Mock
    private Message followupMessage;
    @Captor
    private ArgumentCaptor<InteractionFollowupCreateSpec> followupSpecCaptor;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EventInfoCommand(
                permissionChecker,
                userSessionValidator,
                messageSource,
                eventService,
                organizerEventFormatter
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void eventInfoInteraction_ReturnsFormattedEventDetails_WhenEventExistsInGuild() {
        // Arrange
        Event event = mock(Event.class);
        stubGuildInteraction();
        when(interactionEvent.getOption("message-id")).thenReturn(Optional.of(stringOption("123")));
        when(eventService.getEventByIdForGuild("123", "111")).thenReturn(Optional.of(event));
        when(organizerEventFormatter.formatEventDetails(event)).thenReturn("Event details");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.eventInfoInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("Event details");
    }

    @Test
    void eventInfoInteraction_RepliesEventNotFound_WhenEventDoesNotExistInGuild() {
        // Arrange
        stubGuildInteraction();
        when(interactionEvent.getOption("message-id")).thenReturn(Optional.of(stringOption("123")));
        when(eventService.getEventByIdForGuild("123", "111")).thenReturn(Optional.empty());
        when(messageSource.getMessage("interaction.event-info.event-not-found", null, Locale.ENGLISH))
                .thenReturn("Event not found in this server.");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.eventInfoInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("Event not found in this server.");
    }

    private void stubGuildInteraction() {
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.of(Snowflake.of("111")));
    }

    private void stubFollowup() {
        when(interactionEvent.createFollowup(followupSpecCaptor.capture())).thenReturn(Mono.just(followupMessage));
    }

    private void assertFollowup(String expectedContent) {
        InteractionFollowupCreateSpec spec = followupSpecCaptor.getValue();
        assertThat(spec.contentOrElse("")).isEqualTo(expectedContent);
        assertThat(spec.ephemeralOrElse(false)).isTrue();
    }

    private ApplicationCommandInteractionOption stringOption(String rawValue) {
        ApplicationCommandInteractionOptionData data = ApplicationCommandInteractionOptionData.builder()
                .name("message-id")
                .type(3)
                .value(rawValue)
                .build();
        return new ApplicationCommandInteractionOption(client, data, 111L, null);
    }
}
