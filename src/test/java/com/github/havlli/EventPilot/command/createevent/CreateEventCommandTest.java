package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateMono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateEventCommandTest {

    private AutoCloseable autoCloseable;
    private CreateEventCommand underTest;
    @Mock
    private CreateEventInteraction eventInteraction;
    @Mock
    private SimplePermissionChecker simplePermissionChecker;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CreateEventCommand(eventInteraction, simplePermissionChecker);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void handle_ReplyWithCreateEventMessage() {
        // Arrange
        ChatInputInteractionEvent interactionEvent = mock(ChatInputInteractionEvent.class);
        when(interactionEvent.getCommandName()).thenReturn(underTest.getName());

        InteractionCallbackSpecDeferReplyMono replyMock = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(replyMock);
        InteractionCallbackSpecDeferReplyMono replyEphemeralMock = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(replyMock.withEphemeral(true)).thenReturn(replyEphemeralMock);
        when(replyEphemeralMock.then(any())).thenReturn(Mono.empty());

        InteractionFollowupCreateMono followupMock = mock(InteractionFollowupCreateMono.class);
        when(interactionEvent.createFollowup(anyString())).thenReturn(followupMock);
        InteractionFollowupCreateMono followupEphemeralMock = mock(InteractionFollowupCreateMono.class);
        when(followupMock.withEphemeral(true)).thenReturn(followupEphemeralMock);

        // Act
        StepVerifier.create(underTest.handle(interactionEvent))
                .verifyComplete();
    }

    @Test
    public void handle_ReturnsEmptyMono_WhenCommandNameIsNotEqual() {
        // Arrange
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(event.getCommandName()).thenReturn("not-create-event");

        // Assert
        StepVerifier.create(underTest.handle(event))
                .expectComplete()
                .verify();

        var expected = Mono.empty().block();
        var actual = underTest.handle(event).block();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void setEventType() {
        // Arrange
        Class<? extends Event> expected = InteractionCreateEvent.class;

        // Act
        underTest.setEventType(expected);

        // Assert
        Class<? extends Event> actual = underTest.getEventType();
        assertThat(actual).isEqualTo(expected);
    }
}