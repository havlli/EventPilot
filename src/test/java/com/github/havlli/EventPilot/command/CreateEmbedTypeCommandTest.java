package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateEmbedTypeCommandTest {

    private AutoCloseable autoCloseable;
    private CreateEmbedTypeCommand underTest;
    @Mock
    private SimplePermissionValidator permissionValidatorMock;
    @Mock
    private UserSessionValidator userSessionValidatorMock;
    @Mock
    private CreateEmbedTypeInteraction createEmbedTypeInteractionMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CreateEmbedTypeCommand(
                permissionValidatorMock,
                userSessionValidatorMock,
                createEmbedTypeInteractionMock
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ReturnsEmptyMono_WhenCommandCustomIdIsNotEqual() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        when(eventMock.getCommandName()).thenReturn("not-equal");

        // Act
        Mono<?> actual = underTest.handle(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void handle_ReturnsMessageMono_WhenCommandCustomIdIsEqual() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        when(eventMock.getCommandName()).thenReturn(underTest.getName());
        InteractionCallbackSpecDeferReplyMono callbackSpecDeferReplyMonoMock = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(eventMock.deferReply()).thenReturn(callbackSpecDeferReplyMonoMock);
        when(callbackSpecDeferReplyMonoMock.withEphemeral(true)).thenReturn(callbackSpecDeferReplyMonoMock);
        when(callbackSpecDeferReplyMonoMock.then(any())).thenReturn(Mono.empty());

        doReturn(Mono.empty()).when(createEmbedTypeInteractionMock).initiateOn(eventMock);
        doReturn(Mono.empty()).when(permissionValidatorMock).followupWith(any(), any(), any());
        doReturn(Mono.empty()).when(userSessionValidatorMock).validateThenWrap(any(), any());

        when(eventMock.createFollowup(any(InteractionFollowupCreateSpec.class))).thenReturn(Mono.empty());

        // Act
        Mono<?> actual = underTest.handle(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(permissionValidatorMock, times(1)).followupWith(any(), any(), any());
        verify(userSessionValidatorMock, times(1)).validateThenWrap(any(), any());
    }

    @Test
    void getName() {
        // Arrange
        String expected = "create-embed-type";

        // Act
        String actual = underTest.getName();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getEventType() {
        // Arrange
        Class<? extends Event> expected = ChatInputInteractionEvent.class;

        // Act
        Class<? extends Event> actual = underTest.getEventType();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void setEventType() {
        // Arrange
        Class<? extends Event> expected = ButtonInteractionEvent.class;

        // Act
        underTest.setEventType(expected);

        // Assert
        assertThat(underTest.getEventType()).isEqualTo(expected);
    }
}