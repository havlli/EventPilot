package com.github.havlli.EventPilot.command.test;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TestCommandTest {

    private AutoCloseable autoCloseable;

    private TestCommand underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new TestCommand();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ShouldReplyWithUsername() {
        // Arrange
        String actualUsername = "username";
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        InteractionApplicationCommandCallbackReplyMono replyMono = mock(InteractionApplicationCommandCallbackReplyMono.class);
        when(event.reply()).thenReturn(replyMono);
        InteractionApplicationCommandCallbackReplyMono replyEphemeralMono = mock(InteractionApplicationCommandCallbackReplyMono.class);
        when(replyMono.withEphemeral(true)).thenReturn(replyEphemeralMono);
        InteractionApplicationCommandCallbackReplyMono replyContextMono = mock(InteractionApplicationCommandCallbackReplyMono.class);
        replyContextMono.withContent(actualUsername);
        when(replyEphemeralMono.withContent(anyString())).thenReturn(replyContextMono);

        Interaction interactionMock = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getUsername()).thenReturn(actualUsername);


        // Act
        underTest.handle(event);

        // Assert
        verify(event, times(1)).reply();
        verify(event.reply(), times(1)).withEphemeral(true);
        verify(event.reply().withEphemeral(true), times(1)).withContent(anyString());
    }
}