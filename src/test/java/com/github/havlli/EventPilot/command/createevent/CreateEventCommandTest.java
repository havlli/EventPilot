package com.github.havlli.EventPilot.command.createevent;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class CreateEventCommandTest {

    private AutoCloseable autoCloseable;
    private CreateEventCommand underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CreateEventCommand();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ShouldReplyWithCreateEventMessage() {
        // Arrange
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        InteractionApplicationCommandCallbackReplyMono replyMock = mock(InteractionApplicationCommandCallbackReplyMono.class);
        when(event.reply()).thenReturn(replyMock);
        InteractionApplicationCommandCallbackReplyMono replyMockEphemeral = mock(InteractionApplicationCommandCallbackReplyMono.class);
        when(replyMock.withEphemeral(true)).thenReturn(replyMockEphemeral);
        when(replyMockEphemeral.withContent(anyString())).thenReturn(mock(InteractionApplicationCommandCallbackReplyMono.class));

        // Act
        underTest.handle(event);

        // Assert
        verify(event, times(1)).reply();
        verify(event.reply(), times(1)).withEphemeral(true);
        verify(event.reply().withEphemeral(true), times(1)).withContent("Create Event!");
    }
}