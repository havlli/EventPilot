package com.github.havlli.EventPilot.command.test;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(event.getCommandName()).thenReturn("test");
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

    @Test
    void handle_ShouldTerminate() {
        // Arrange
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(event.getCommandName()).thenReturn("not-test");

        // Act
        Mono<?> handle = underTest.handle(event);

        // Assert
        StepVerifier.create(handle)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void canSetEventType() {
        // Arrange
        TestCommand underTest = new TestCommand();

        // Act
        underTest.setEventType(ChatInputInteractionEvent.class);

        // Assert
        assertThat(underTest.getEventType()).isEqualTo(ChatInputInteractionEvent.class);
    }
}