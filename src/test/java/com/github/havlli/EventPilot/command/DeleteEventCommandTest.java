package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.rest.util.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeleteEventCommandTest {

    private AutoCloseable autoCloseable;
    private DeleteEventCommand underTest;
    @Mock
    private SimplePermissionChecker permissionChecker;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private MessageChannel messageChannel;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new DeleteEventCommand(permissionChecker);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ReturnsEmptyMono_WhenCommandNamesNotEqual() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("not-delete-event");

        // Act
        Mono<?> result = underTest.handle(interactionEvent);

        // Assert
        StepVerifier.create(result)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void handle_ReturnsEventMono_WhenCommandNamesAreEqual() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("delete-event");
        InteractionCallbackSpecDeferReplyMono deferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(deferReplyMono);
        when(deferReplyMono.withEphemeral(true)).thenReturn(deferReplyMono);
        when(permissionChecker.followupWith(
                eq(interactionEvent),
                eq(Permission.MANAGE_CHANNELS),
                any()
        )).thenReturn(Mono.empty());
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));

        // Act
        underTest.handle(interactionEvent);

        // Assert
        verify(permissionChecker,times(1))
                .followupWith(eq(interactionEvent), eq(Permission.MANAGE_CHANNELS), any());
    }
}