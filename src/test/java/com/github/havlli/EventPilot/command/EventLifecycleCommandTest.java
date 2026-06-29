package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.DiscordService;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventStatus;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.rest.util.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventLifecycleCommandTest {

    private AutoCloseable autoCloseable;
    private CloseEventCommand underTest;
    @Mock
    private SimplePermissionValidator permissionChecker;
    @Mock
    private UserSessionValidator userSessionValidator;
    @Mock
    private MessageSource messageSource;
    @Mock
    private EventService eventService;
    @Mock
    private DiscordService discordService;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CloseEventCommand(permissionChecker, userSessionValidator, messageSource, eventService, discordService);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ReturnsEmptyMono_WhenCommandNameDoesNotMatch() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("cancel-event");

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(eventService);
    }

    @Test
    void handle_UpdatesStatusRefreshesMessageAndReplies_WhenEventExists() {
        // Arrange
        com.github.havlli.EventPilot.entity.event.Event event = createEvent("0");
        Message followupMessage = mock(Message.class);
        Message updatedMessage = mock(Message.class);
        stubValidatedInteraction();
        when(eventService.updateStatusIfExists("0", EventStatus.CLOSED)).thenReturn(Optional.of(event));
        when(discordService.updateEventMessage(event)).thenReturn(Mono.just(updatedMessage));
        stubFollowup("interaction.lifecycle.closed", "Event closed!", followupMessage);

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        verify(eventService, times(1)).updateStatusIfExists("0", EventStatus.CLOSED);
        verify(discordService, times(1)).updateEventMessage(event);
    }

    @Test
    void handle_RepliesEventNotFound_WhenEventDoesNotExist() {
        // Arrange
        Message followupMessage = mock(Message.class);
        stubValidatedInteraction();
        when(eventService.updateStatusIfExists("0", EventStatus.CLOSED)).thenReturn(Optional.empty());
        stubFollowup("interaction.lifecycle.event-not-found", "Event not found!", followupMessage);

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        verifyNoInteractions(discordService);
    }

    @Test
    void handle_RepliesSuccess_WhenMessageRefreshFailsAfterDatabaseUpdate() {
        // Arrange
        com.github.havlli.EventPilot.entity.event.Event event = createEvent("0");
        Message followupMessage = mock(Message.class);
        stubValidatedInteraction();
        when(eventService.updateStatusIfExists("0", EventStatus.CLOSED)).thenReturn(Optional.of(event));
        when(discordService.updateEventMessage(event)).thenReturn(Mono.error(new RuntimeException("discord unavailable")));
        stubFollowup("interaction.lifecycle.closed", "Event closed!", followupMessage);

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        verify(eventService, times(1)).updateStatusIfExists("0", EventStatus.CLOSED);
    }

    @Test
    void getName_ReturnsCloseEvent() {
        assertThat(underTest.getName()).isEqualTo("close-event");
    }

    private void stubValidatedInteraction() {
        when(interactionEvent.getCommandName()).thenReturn("close-event");
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

    private void stubFollowup(String messageKey, String message, Message followupMessage) {
        when(messageSource.getMessage(messageKey, null, Locale.ENGLISH)).thenReturn(message);
        when(interactionEvent.createFollowup(message)).thenReturn(InteractionFollowupCreateMono.of(interactionEvent));
        when(interactionEvent.createFollowup(any(InteractionFollowupCreateSpec.class))).thenReturn(Mono.just(followupMessage));
    }

    private com.github.havlli.EventPilot.entity.event.Event createEvent(String eventId) {
        return new com.github.havlli.EventPilot.entity.event.Event(
                eventId,
                "name",
                "description",
                "author",
                Instant.now().plusSeconds(3600),
                "123",
                null,
                "25",
                new ArrayList<>(),
                new Guild("1", "guild"),
                new EmbedType(1L, "default", "{}", null)
        );
    }
}
