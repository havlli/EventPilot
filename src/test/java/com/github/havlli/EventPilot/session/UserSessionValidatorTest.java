package com.github.havlli.EventPilot.session;

import com.github.havlli.EventPilot.core.MessageCreator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.Mockito.*;

class UserSessionValidatorTest {

    private AutoCloseable autoCloseable;
    private UserSessionValidator underTest;
    @Mock
    private UserSessionService userSessionServiceMock;
    @Mock
    private MessageCreator messageCreatorMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new UserSessionValidator(userSessionServiceMock, messageCreatorMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void validate_WrapsMessageToSessionAndTerminatesSessionOnComplete_WhenUserSessionExists() {
        // Arrange
        Message messageMock = mock(Message.class);
        Mono<Message> followupMessageMock = Mono.just(messageMock);
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(Snowflake.of(1234));
        when(userMock.getUsername()).thenReturn("test");

        when(userSessionServiceMock.createUserSession("1234", "test")).thenReturn(Optional.of(new UserSession("1234", "test")));
        UserSessionValidator underTestSpy = spy(underTest);

        // Act
        Mono<Message> actual = underTestSpy.validateThenWrap(followupMessageMock, eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageMock)
                .verifyComplete();
        verify(underTestSpy, times(1)).terminate(eventMock);
    }

    @Test
    void validate_ReturnsSessionAlreadyActiveMessage_WhenUserSessionIsEmpty() {
        // Arrange
        Mono<Message> followupMessageMock = mock(Mono.class);
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(Snowflake.of(1234));
        when(userMock.getUsername()).thenReturn("test");

        when(userSessionServiceMock.createUserSession("1234", "test")).thenReturn(Optional.empty());

        // Act
        underTest.validateThenWrap(followupMessageMock, eventMock);

        // Assert
        verify(messageCreatorMock, times(1)).sessionAlreadyActive(eventMock);
    }

    @Test
    void terminate() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(Snowflake.of(1234));

        // Act
        underTest.terminate(eventMock);

        // Assert
        verify(userSessionServiceMock, only()).terminateUserSession("1234");
    }
}