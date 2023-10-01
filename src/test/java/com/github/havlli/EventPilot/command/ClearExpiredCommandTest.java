package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.component.SelectMenu;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClearExpiredCommandTest {

    private AutoCloseable autoCloseable;
    private ClearExpiredCommand underTest;

    @Mock
    private SimplePermissionValidator permissionCheckerMock;
    @Mock
    private SelectMenuComponent selectMenuComponentMock;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private MessageChannel messageChannel;
    @Mock
    private UserSessionValidator sessionValidatorMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new ClearExpiredCommand(permissionCheckerMock, selectMenuComponentMock, sessionValidatorMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ReturnsEmptyMono_WhenCommandNamesNotEquals() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("not-clear-expired");

        // Act
        Mono<?> resultMono = underTest.handle(interactionEvent);

        // Assert
        StepVerifier.create(resultMono)
                .verifyComplete();
    }

    @Test
    void handle_ReturnsEventMonoAndDeletesSession_WhenCommandNamesAreEquals() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("clear-expired");
        InteractionCallbackSpecDeferReplyMono deferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(deferReplyMono);
        when(deferReplyMono.withEphemeral(true)).thenReturn(deferReplyMono);
        Message messageMock = mock(Message.class);
        when(deferReplyMono.then(any())).thenReturn(Mono.just(messageMock));

        when(sessionValidatorMock.validate(any(), eq(interactionEvent))).thenReturn(Mono.just(messageMock));

        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        when(messageChannel.getMessagesAfter(Snowflake.of(0))).thenReturn(Flux.just(messageMock));

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageMock)
                .verifyComplete();
        verify(sessionValidatorMock, times(1)).terminate(eq(interactionEvent));
        verify(permissionCheckerMock, times(1))
                .followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS));
    }

    @Test
    void sendMessage() {
        // Arrange
        String response = "response";
        InteractionFollowupCreateMono expected = mock(InteractionFollowupCreateMono.class);
        when(interactionEvent.createFollowup(response)).thenReturn(expected);

        // Act
        underTest.sendResponse(interactionEvent, response);

        // Assert
        verify(interactionEvent, only()).createFollowup(response);
    }

    @Test
    void filterBotMessagesPredicate_ReturnsTrue_WhenAuthorIsBot() {
        // Arrange
        Message messageMock = mock(Message.class);
        User userMock = mock(User.class);
        when(messageMock.getAuthor()).thenReturn(Optional.of(userMock));
        when(userMock.isBot()).thenReturn(true);

        // Act
        boolean actual = underTest.filterBotMessages().test(messageMock);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void filterBotMessagesPredicate_ReturnsFalse_WhenAuthorIsNotBot() {
        // Arrange
        Message messageMock = mock(Message.class);
        User userMock = mock(User.class);
        when(messageMock.getAuthor()).thenReturn(Optional.of(userMock));
        when(userMock.isBot()).thenReturn(false);

        // Act
        boolean actual = underTest.filterBotMessages().test(messageMock);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void filterExpiredPredicate_ReturnsTrue_WhenAnyComponentsMatchExpiredComponent() {
        // Arrange
        Message messageMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        List<LayoutComponent> layoutComponentList = List.of(layoutComponentMock);
        when(messageMock.getComponents()).thenReturn(layoutComponentList);

        SelectMenu selectComponentMock = mock(SelectMenu.class);
        List<MessageComponent> messageComponentList = List.of(selectComponentMock);
        when(layoutComponentMock.getChildren()).thenReturn(messageComponentList);
        when(selectComponentMock.getCustomId()).thenReturn("expired");
        when(selectMenuComponentMock.getCustomId()).thenReturn("expired");

        // Act
        boolean actual = underTest.filterExpired().test(messageMock);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void filterExpiredPredicate_ReturnsFalse_WhenNoComponentsMatchesExpired() {
        // Arrange
        Message messageMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        List<LayoutComponent> layoutComponentList = List.of(layoutComponentMock);
        when(messageMock.getComponents()).thenReturn(layoutComponentList);

        SelectMenu selectComponentMock = mock(SelectMenu.class);
        List<MessageComponent> messageComponentList = List.of(selectComponentMock);
        when(layoutComponentMock.getChildren()).thenReturn(messageComponentList);
        when(selectComponentMock.getCustomId()).thenReturn("component");
        when(selectMenuComponentMock.getCustomId()).thenReturn("expired");

        // Act
        boolean actual = underTest.filterExpired().test(messageMock);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void filterExpiredPredicate_ReturnsFalse_WhenNoMessageComponentsExistsInMessage() {
        // Arrange
        Message messageMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        List<LayoutComponent> layoutComponentList = List.of(layoutComponentMock);
        when(messageMock.getComponents()).thenReturn(layoutComponentList);

        List<MessageComponent> messageComponentList = List.of();
        when(layoutComponentMock.getChildren()).thenReturn(messageComponentList);

        // Act
        boolean actual = underTest.filterExpired().test(messageMock);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void filterExpiredPredicate_ReturnsFalse_WhenNoLayoutComponentsExistsInMessage() {
        // Arrange
        Message messageMock = mock(Message.class);
        List<LayoutComponent> layoutComponentList = List.of();
        when(messageMock.getComponents()).thenReturn(layoutComponentList);

        // Act
        boolean actual = underTest.filterExpired().test(messageMock);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void getName() {
        // Arrange
        String expected = "clear-expired";

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

    @Test
    void formatResponseMessage_ReturnsNoExpiredEventsMessage_WhenCountIsZero() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        Class<?> underTestClass = ClearExpiredCommand.class;
        Method formatResponseMessageMethod = underTestClass.getDeclaredMethod("formatResponseMessage", int.class);
        formatResponseMessageMethod.setAccessible(true);

        String expected = "No expired events found in this channel.";

        // Act
        String actual = (String) formatResponseMessageMethod.invoke(underTest, 0);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void formatResponseMessage_ReturnsResponseMessage_WhenCountIsNotZero() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        Class<?> underTestClass = ClearExpiredCommand.class;
        Method formatResponseMessageMethod = underTestClass.getDeclaredMethod("formatResponseMessage", int.class);
        formatResponseMessageMethod.setAccessible(true);

        String expected = "Deleted 2 events in this channel.";

        // Act
        String actual = (String) formatResponseMessageMethod.invoke(underTest, 2);

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void handleResponse_formatsResponseAndSendsResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        ClearExpiredCommand underTestSpy = spy(underTest);
        Class<?> underTestClass = ClearExpiredCommand.class;
        Method handleResponseMethod = underTestClass.getDeclaredMethod("handleResponse", ChatInputInteractionEvent.class);
        handleResponseMethod.setAccessible(true);
        Function<List<Message>, Mono<? extends Message>> handleResponseFunction =
                (Function<List<Message>, Mono<? extends Message>>) handleResponseMethod.invoke(underTestSpy, interactionEvent);

        doReturn(Mono.empty()).when(underTestSpy).sendResponse(any(), any());

        // Act
        Mono<? extends Message> actual = handleResponseFunction.apply(List.of(mock(Message.class)));

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void filterAllMessagesThenDelete_ReturnsDeleteMessages() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        Class<?> underTestClass = ClearExpiredCommand.class;
        ClearExpiredCommand underTestSpy = spy(underTest);
        Method filterAllMessagesThenDeleteMethod = underTestClass.getDeclaredMethod("filterAllMessagesThenDelete", MessageChannel.class);
        filterAllMessagesThenDeleteMethod.setAccessible(true);

        Message messageMock = mock(Message.class);
        when(messageChannel.getMessagesAfter(Snowflake.of(0))).thenReturn(Flux.just(messageMock));
        Predicate<Message> ignorePredicate = message -> true;
        doReturn(ignorePredicate).when(underTestSpy).filterBotMessages();
        doReturn(ignorePredicate).when(underTestSpy).filterExpired();

        // Act
        Flux<Message> actual = (Flux<Message>) filterAllMessagesThenDeleteMethod.invoke(underTest, messageChannel);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void deleteMessageThenReturn_InvokesDeletionThenReturnsSameObject() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        Class<?> underTestClass = ClearExpiredCommand.class;
        Method deleteMessageThenReturnMethod = underTestClass.getDeclaredMethod("deleteMessageThenReturn", Message.class);
        deleteMessageThenReturnMethod.setAccessible(true);

        Message messageMock = mock(Message.class);
        when(messageMock.delete()).thenReturn(Mono.empty());

        // Act
        Mono<Message> actual = (Mono<Message>) deleteMessageThenReturnMethod.invoke(underTest, messageMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNext(messageMock)
                .verifyComplete();
    }
}