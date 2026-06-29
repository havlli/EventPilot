package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.session.UserSessionValidator;
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
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class DeleteEventCommandTest {

    private AutoCloseable autoCloseable;
    private DeleteEventCommand underTest;
    @Mock
    private SimplePermissionValidator permissionChecker;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private MessageChannel messageChannel;
    @Mock
    private UserSessionValidator sessionValidatorMock;
    @Mock
    private MessageSource messageSourceMock;
    @Mock
    private EventService eventServiceMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new DeleteEventCommand(permissionChecker, sessionValidatorMock, messageSourceMock, eventServiceMock);
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
    void handle_ReturnsEventMonoAndCallsValidators_WhenCommandNamesAreEqual() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("delete-event");
        InteractionCallbackSpecDeferReplyMono deferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(deferReplyMono);
        when(deferReplyMono.withEphemeral(true)).thenReturn(deferReplyMono);
        Message messageMock = mock(Message.class);
        when(deferReplyMono.then(any())).thenReturn(Mono.just(messageMock));
        when(sessionValidatorMock.validateThenWrap(any(), eq(interactionEvent))).thenReturn(Mono.just(messageMock));

        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageMock)
                .verifyComplete();
        verify(sessionValidatorMock, times(1)).validateThenWrap(any(), eq(interactionEvent));
        verify(permissionChecker, times(1))
                .followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS));
    }

    @Test
    void deleteEventInteraction_ReturnsEventDeletedMessage_WhenMessageMissingAndDatabaseEventExists() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        ClientException notFoundError = clientExceptionWithStatus(HttpResponseStatus.NOT_FOUND);
        when(messageChannel.getMessageById(any())).thenReturn(Mono.error(notFoundError));
        when(eventServiceMock.deleteEventIfExists("0")).thenReturn(true);

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event deleted!");
        when(messageSourceMock.getMessage("interaction.delete-event.event-deleted", null, Locale.ENGLISH))
                .thenReturn("Event deleted!");

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(eventServiceMock, times(1)).deleteEventIfExists("0");
        verify(underTestSpy, times(1)).sendMessage(interactionEvent, "Event deleted!");
    }

    @Test
    void deleteEventInteraction_ReturnsEventNotFoundMessage_WhenMessageMissingAndDatabaseEventDoesNotExist() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        ClientException notFoundError = clientExceptionWithStatus(HttpResponseStatus.NOT_FOUND);
        when(messageChannel.getMessageById(any())).thenReturn(Mono.error(notFoundError));
        when(eventServiceMock.deleteEventIfExists("0")).thenReturn(false);

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event not found!");
        when(messageSourceMock.getMessage("interaction.delete-event.event-not-found", null, Locale.ENGLISH))
                .thenReturn("Event not found!");

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(eventServiceMock, times(1)).deleteEventIfExists("0");
        verify(underTestSpy, times(1)).sendMessage(interactionEvent, "Event not found!");
    }

    @Test
    void deleteEventInteraction_PropagatesDiscordErrorAndDoesNotDeleteDatabase_WhenMessageFetchFailsForNonNotFound() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        ClientException discordError = clientExceptionWithStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        when(messageChannel.getMessageById(any())).thenReturn(Mono.error(discordError));

        // Act
        Mono<Message> actual = underTest.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectError(ClientException.class)
                .verify();
        verifyNoInteractions(eventServiceMock);
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
        when(messageMock.getId()).thenReturn(Snowflake.of("9999"));
        when(userMock.getId()).thenReturn(Snowflake.of("2345"));

        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        when(interactionEvent.getClient()).thenReturn(clientMock);
        when(clientMock.getSelfId()).thenReturn(Snowflake.of("1234"));

        when(messageSourceMock.getMessage("interaction.delete-event.not-authorized", null, Locale.ENGLISH))
                .thenReturn("Event not found, already deleted or not posted by this bot!");

        DeleteEventCommand underTestSpy = spy(underTest);
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event not found, already deleted or not posted by this bot!");

        // Act
        Mono<Message> actual = underTestSpy.deleteEventInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(underTestSpy, times(1)).sendMessage(interactionEvent, "Event not found, already deleted or not posted by this bot!");
        verifyNoInteractions(eventServiceMock);
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
        when(messageSourceMock.getMessage("interaction.delete-event.not-authorized", null, Locale.ENGLISH))
                .thenReturn("Event not found, already deleted or not posted by this bot!");
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
        when(messageMock.getId()).thenReturn(Snowflake.of("1234"));
        when(eventServiceMock.deleteEventIfExists("1234")).thenReturn(false);

        DeleteEventCommand underTestSpy = spy(underTest);
        when(messageSourceMock.getMessage("interaction.delete-event.event-deleted", null, Locale.ENGLISH))
                .thenReturn("Event deleted!");
        doReturn(Mono.empty()).when(underTestSpy).sendMessage(interactionEvent, "Event deleted!");
        // Act
        Mono<Message> actual = underTestSpy.deleteMessage(interactionEvent, messageMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(messageMock, times(1)).delete();
        verify(eventServiceMock, times(1)).deleteEventIfExists("1234");
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

    private ClientException clientExceptionWithStatus(HttpResponseStatus status) {
        ClientException exception = mock(ClientException.class);
        when(exception.getStatus()).thenReturn(status);
        return exception;
    }
}
