package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Permission;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void handle_ReturnsEmptyMono_WhenCommandNameDoesNotMatch() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("list-events");

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(permissionChecker, userSessionValidator, eventService);
    }

    @Test
    void handle_DelegatesThroughPermissionAndSessionValidators_WhenCommandNameMatches() {
        // Arrange
        Event event = mock(Event.class);
        stubValidatedHandle();
        when(interactionEvent.getOption("message-id")).thenReturn(Optional.of(stringOption("123")));
        when(eventService.getEventByIdForGuild("123", "111")).thenReturn(Optional.of(event));
        when(organizerEventFormatter.formatEventDetails(event)).thenReturn("Event details");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        verify(permissionChecker, times(1)).followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS));
        verify(userSessionValidator, times(1)).validateThenWrap(any(), eq(interactionEvent));
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

    @Test
    void eventInfoInteraction_RepliesGuildOnly_WhenInteractionIsNotFromGuild() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.empty());
        when(messageSource.getMessage("interaction.organizer.guild-only", null, Locale.ENGLISH))
                .thenReturn("This command can only be used in a Discord server.");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.eventInfoInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("This command can only be used in a Discord server.");
        verifyNoInteractions(eventService);
    }

    @Test
    void eventInfoInteraction_UsesFallbackMessageId_WhenOptionIsMissing() {
        // Arrange
        Event event = mock(Event.class);
        stubGuildInteraction();
        when(interactionEvent.getOption("message-id")).thenReturn(Optional.empty());
        when(eventService.getEventByIdForGuild("0", "111")).thenReturn(Optional.of(event));
        when(organizerEventFormatter.formatEventDetails(event)).thenReturn("Fallback event details");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.eventInfoInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("Fallback event details");
    }

    @Test
    void commandMetadata_CanBeReadAndUpdated() {
        // Assert
        assertThat(underTest.getName()).isEqualTo("event-info");
        assertThat(underTest.getEventType()).isEqualTo(ChatInputInteractionEvent.class);

        // Act
        underTest.setEventType(ReadyEvent.class);

        // Assert
        assertThat(underTest.getEventType()).isEqualTo(ReadyEvent.class);
    }

    private void stubValidatedHandle() {
        when(interactionEvent.getCommandName()).thenReturn("event-info");
        stubGuildInteraction();
        InteractionCallbackSpecDeferReplyMono deferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(deferReplyMono);
        when(deferReplyMono.withEphemeral(true)).thenReturn(deferReplyMono);
        when(deferReplyMono.then(org.mockito.ArgumentMatchers.<Mono<Message>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionValidator.validateThenWrap(any(), eq(interactionEvent)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionChecker.followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
