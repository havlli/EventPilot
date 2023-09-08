package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.component.SelectMenuComponent;
import com.github.havlli.EventPilot.core.SimplePermissionChecker;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClearExpiredCommandTest {

    private AutoCloseable autoCloseable;
    private ClearExpiredCommand underTest;

    @Mock
    private SimplePermissionChecker permissionCheckerMock;
    @Mock
    private SelectMenuComponent selectMenuComponentMock;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private MessageChannel messageChannel;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new ClearExpiredCommand(permissionCheckerMock, selectMenuComponentMock);
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
    void handle_ReturnsEventMono_WhenCommandNamesAreEquals() {
        // Arrange
        when(interactionEvent.getCommandName()).thenReturn("clear-expired");
        InteractionCallbackSpecDeferReplyMono deferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(deferReplyMono);
        when(deferReplyMono.withEphemeral(true)).thenReturn(deferReplyMono);
        when(permissionCheckerMock.followupWith(
                eq(interactionEvent),
                eq(Permission.MANAGE_CHANNELS),
                any()
        )).thenReturn(Mono.empty());

        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getChannel()).thenReturn(Mono.just(messageChannel));
        Message messageMock = mock(Message.class);
        when(messageChannel.getMessagesAfter(Snowflake.of(0))).thenReturn(Flux.just(messageMock));

        // Act
        underTest.handle(interactionEvent);

        // Assert
        verify(permissionCheckerMock, times(1))
                .followupWith(eq(interactionEvent), eq(Permission.MANAGE_CHANNELS), any());
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
}