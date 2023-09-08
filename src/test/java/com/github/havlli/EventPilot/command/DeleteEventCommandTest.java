package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateMono;
import discord4j.rest.util.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        verify(permissionChecker, times(1))
                .followupWith(eq(interactionEvent), eq(Permission.MANAGE_CHANNELS), any());
    }

    @Test
    void deleteEventInteraction_ReturnsEventNotFoundMessage_WhenExceptionThrown() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        when(messageChannel.getMessageById(any())).thenReturn(Mono.error(new RuntimeException("Event not found")));

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event not found!");

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(underTestSpy, times(1)).sendMessage(interactionEvent, "Event not found!");
    }

    @Test
    void deleteEventInteraction_DeletesMessageAndReturnsMessage() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        Message messageMock = mock(Message.class);
        when(messageChannel.getMessageById(any())).thenReturn(Mono.just(messageMock));
        User userMock = mock(User.class);
        when(messageMock.getAuthor()).thenReturn(Optional.of(userMock));
        when(userMock.getId()).thenReturn(Snowflake.of("1234"));

        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        when(interactionEvent.getClient()).thenReturn(clientMock);
        when(clientMock.getSelfId()).thenReturn(Snowflake.of("1234"));

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).deleteMessage(interactionEvent, messageMock);

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(underTestSpy, times(1)).deleteMessage(interactionEvent, messageMock);
    }

    @Test
    void deleteEventInteraction_ReturnsNotValidMessage_WhenNotAuthorOfMessage() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        Message messageMock = mock(Message.class);
        when(messageChannel.getMessageById(any())).thenReturn(Mono.just(messageMock));
        User userMock = mock(User.class);
        when(messageMock.getAuthor()).thenReturn(Optional.of(userMock));
        when(userMock.getId()).thenReturn(Snowflake.of("2345"));

        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        when(interactionEvent.getClient()).thenReturn(clientMock);
        when(clientMock.getSelfId()).thenReturn(Snowflake.of("1234"));

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event not found, already deleted or not posted by this bot!");

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(underTestSpy, times(1)).sendMessage(interactionEvent, "Event not found, already deleted or not posted by this bot!");
    }

    @Test
    void deleteEventInteraction_ReturnsNotValidMessage_WhenAlreadyDeleted() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        Message messageMock = mock(Message.class);
        when(messageChannel.getMessageById(any())).thenReturn(Mono.just(messageMock));
        when(messageMock.getAuthor()).thenReturn(Optional.empty());

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event not found, already deleted or not posted by this bot!");

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(underTestSpy, times(1)).sendMessage(interactionEvent, "Event not found, already deleted or not posted by this bot!");
    }

    @Test
    void createMessage() {
        // Arrange
        String content = "message";
        InteractionFollowupCreateMono followupCreateMono = mock(InteractionFollowupCreateMono.class);
        when(interactionEvent.createFollowup(content)).thenReturn(followupCreateMono);
        when(followupCreateMono.withEphemeral(true)).thenReturn(followupCreateMono);

        // Act
        underTest.sendMessage(interactionEvent, content);

        // Assert
        verify(interactionEvent, times(1)).createFollowup(content);
    }

    @Test
    void deleteMessage() {
        // Arrange
        Message messageMock = mock(Message.class);
        when(messageMock.delete()).thenReturn(Mono.empty());

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event deleted!");
        // Act
        Mono<Message> actual = underTestSpy.deleteMessage(interactionEvent, messageMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(messageMock, times(1)).delete();
    }

    @Test
    void getName() {
        // Arrange
        String expected = "delete-event";

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
        Class<? extends Event> expected = InteractionCreateEvent.class;

        // Act
        underTest.setEventType(expected);

        // Assert
        Class<? extends Event> actual = underTest.getEventType();
        assertThat(actual).isEqualTo(expected);
    }
}