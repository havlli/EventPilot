package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ActionRowComponent;
import com.github.havlli.EventPilot.prompt.TextPromptBuilder.PromptType;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.*;
import discord4j.rest.interaction.InteractionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TextPromptBuilderTest {

    private AutoCloseable autoCloseable;
    @Mock
    private GatewayDiscordClient clientMock;
    @Mock
    private MessageChannel messageChannelMock;
    private Mono<MessageChannel> messageChannelMono;
    @Mock
    private MessageCollector messageCollectorMock;
    @Mock
    private ActionRowComponent actionRowComponentMock;
    private MessageCreateSpec messageCreateSpec;


    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        messageChannelMono = Mono.just(messageChannelMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void mono_ReturnsMessageCreateEvent_WhenEventClassIsMessageCreateEventAndPromptTypeDefault() {
        // Arrange
        Class<MessageCreateEvent> eventClass = MessageCreateEvent.class;
        PromptType promptType = PromptType.DEFAULT;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Flux<MessageCreateEvent> messageCreateEventFlux = Flux.just(messageCreateEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(messageCreateEventFlux);

        TextPromptBuilder<MessageCreateEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<MessageCreateEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageCreateEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsSelectMenuInteractionEvent_WhenEventClassIsSelectMenuInteractionEventAndPromptTypeDefault() {
        // Arrange
        Class<SelectMenuInteractionEvent> eventClass = SelectMenuInteractionEvent.class;
        PromptType promptType = PromptType.DEFAULT;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Flux<SelectMenuInteractionEvent> selectMenuInteractionEventFlux = Flux.just(selectMenuInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(selectMenuInteractionEventFlux);

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(selectMenuInteractionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);
        Message selectMenuMessage = mock(Message.class);
        ActionRow actionRow = mock(ActionRow.class);
        Mono<Message> selectMenuMessageMono = Mono.just(selectMenuMessage);
        when(interactionCallbackSpecDeferEditMono.then(selectMenuInteractionEventMock.editReply(InteractionReplyEditSpec.builder()
                .components(List.of(actionRow))
                .build()))
        ).thenReturn(selectMenuMessageMono);

        when(actionRowComponentMock.getDisabledRow()).thenReturn(actionRow);

        TextPromptBuilder<SelectMenuInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .actionRowComponent(actionRowComponentMock)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<SelectMenuInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(selectMenuInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsSelectMenuInteractionEvent_WhenEventClassIsSelectMenuInteractionEventAndPromptTypeDeleteOnResponse() {
        // Arrange
        Class<SelectMenuInteractionEvent> eventClass = SelectMenuInteractionEvent.class;
        PromptType promptType = PromptType.DELETE_ON_RESPONSE;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Flux<SelectMenuInteractionEvent> selectMenuInteractionEventFlux = Flux.just(selectMenuInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(selectMenuInteractionEventFlux);

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(selectMenuInteractionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);

        InteractionResponse interactionResponseMock = mock(InteractionResponse.class);
        when(selectMenuInteractionEventMock.getInteractionResponse()).thenReturn(interactionResponseMock);
        when(interactionResponseMock.deleteInitialResponse()).thenReturn(Mono.empty());
        when(interactionCallbackSpecDeferEditMono.then(selectMenuInteractionEventMock.getInteractionResponse().deleteInitialResponse())).thenReturn(Mono.empty());

        TextPromptBuilder<SelectMenuInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .actionRowComponent(actionRowComponentMock)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<SelectMenuInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(selectMenuInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsSelectMenuInteractionEvent_WhenEventClassIsSelectMenuInteractionEventAndPromptTypeDeferrableEdit() {
        // Arrange
        Class<SelectMenuInteractionEvent> eventClass = SelectMenuInteractionEvent.class;
        PromptType promptType = PromptType.DEFERRABLE_EDIT;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Flux<SelectMenuInteractionEvent> selectMenuInteractionEventFlux = Flux.just(selectMenuInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(selectMenuInteractionEventFlux);

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(selectMenuInteractionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);

        when(interactionCallbackSpecDeferEditMono.then(any())).thenReturn(Mono.empty());

        TextPromptBuilder<SelectMenuInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .actionRowComponent(actionRowComponentMock)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<SelectMenuInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsSelectMenuInteractionEvent_WhenEventClassIsSelectMenuInteractionEventAndPromptTypeDeferrableReply() {
        // Arrange
        Class<SelectMenuInteractionEvent> eventClass = SelectMenuInteractionEvent.class;
        PromptType promptType = PromptType.DEFERRABLE_REPLY;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Flux<SelectMenuInteractionEvent> selectMenuInteractionEventFlux = Flux.just(selectMenuInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(selectMenuInteractionEventFlux);

        InteractionCallbackSpecDeferReplyMono interactionCallbackSpecDeferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(selectMenuInteractionEventMock.deferReply()).thenReturn(interactionCallbackSpecDeferReplyMono);

        Mono<SelectMenuInteractionEvent> selectMenuInteractionEventMockMono = Mono.just(selectMenuInteractionEventMock);
        doReturn(selectMenuInteractionEventMockMono).when(interactionCallbackSpecDeferReplyMono).then(any());

        TextPromptBuilder<SelectMenuInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .actionRowComponent(actionRowComponentMock)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<SelectMenuInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(selectMenuInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsButtonInteractionEvent_WhenEventClassIsButtonInteractionEventAndPromptTypeDefault() {
        // Arrange
        Class<ButtonInteractionEvent> eventClass = ButtonInteractionEvent.class;
        PromptType promptType = PromptType.DEFAULT;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Flux<ButtonInteractionEvent> buttonInteractionEventMockFlux = Flux.just(buttonInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(buttonInteractionEventMockFlux);

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(buttonInteractionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);
        Message buttonMenuMessage = mock(Message.class);
        Mono<Message> buttonMenuMessageMono = Mono.just(buttonMenuMessage);
        when(interactionCallbackSpecDeferEditMono.then(buttonInteractionEventMock.editReply(InteractionReplyEditSpec.builder()
                .components(List.of())
                .build()))
        ).thenReturn(buttonMenuMessageMono);

        TextPromptBuilder<ButtonInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<ButtonInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(buttonInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsButtonInteractionEvent_WhenEventClassIsButtonInteractionEventAndPromptTypeDeleteOnResponse() {
        // Arrange
        Class<ButtonInteractionEvent> eventClass = ButtonInteractionEvent.class;
        PromptType promptType = PromptType.DELETE_ON_RESPONSE;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Flux<ButtonInteractionEvent> buttonInteractionEventMockFlux = Flux.just(buttonInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(buttonInteractionEventMockFlux);

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(buttonInteractionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);

        InteractionResponse interactionResponseMock = mock(InteractionResponse.class);
        when(buttonInteractionEventMock.getInteractionResponse()).thenReturn(interactionResponseMock);
        when(interactionResponseMock.deleteInitialResponse()).thenReturn(Mono.empty());
        when(interactionCallbackSpecDeferEditMono.then(buttonInteractionEventMock.getInteractionResponse().deleteInitialResponse())).thenReturn(Mono.empty());

        TextPromptBuilder<ButtonInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<ButtonInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(buttonInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsButtonInteractionEvent_WhenEventClassIsButtonInteractionEventAndPromptTypeDeferrableEdit() {
        // Arrange
        Class<ButtonInteractionEvent> eventClass = ButtonInteractionEvent.class;
        PromptType promptType = PromptType.DEFERRABLE_EDIT;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Flux<ButtonInteractionEvent> buttonInteractionEventMockFlux = Flux.just(buttonInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(buttonInteractionEventMockFlux);

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(buttonInteractionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);

        Mono<ButtonInteractionEvent> buttonInteractionEventMono = Mono.just(buttonInteractionEventMock);
        doReturn(buttonInteractionEventMono).when(interactionCallbackSpecDeferEditMono).then(any());

        TextPromptBuilder<ButtonInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<ButtonInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(buttonInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void mono_ReturnsButtonInteractionEvent_WhenEventClassIsButtonInteractionEventAndPromptTypeDeferrableReply() {
        // Arrange
        Class<ButtonInteractionEvent> eventClass = ButtonInteractionEvent.class;
        PromptType promptType = PromptType.DEFERRABLE_REPLY;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Flux<ButtonInteractionEvent> buttonInteractionEventMockFlux = Flux.just(buttonInteractionEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(buttonInteractionEventMockFlux);

        InteractionCallbackSpecDeferReplyMono interactionCallbackSpecDeferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(buttonInteractionEventMock.deferReply()).thenReturn(interactionCallbackSpecDeferReplyMono);

        Mono<ButtonInteractionEvent> buttonInteractionEventMono = Mono.just(buttonInteractionEventMock);
        doReturn(buttonInteractionEventMono).when(interactionCallbackSpecDeferReplyMono).then(any());

        TextPromptBuilder<ButtonInteractionEvent> textPromptBuilder = new TextPromptBuilder.Builder<>(clientMock, eventClass)
                .withPromptType(promptType)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Act
        Mono<ButtonInteractionEvent> actual = textPromptBuilder.createMono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(buttonInteractionEventMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).collect(messageMock);
    }

    @Test
    void builder_ReturnsDefaultMessageCreateEvent_WhenBuildMethodCalled() {
        // Act
        TextPromptBuilder<MessageCreateEvent> actual = new TextPromptBuilder.Builder<>(clientMock, MessageCreateEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Assert
        String[] expectedNullFields = {
                "errorClass",
                "onErrorMessage",
                "actionRowComponent"
        };
        assertThat(actual).hasNoNullFieldsOrPropertiesExcept(expectedNullFields);
    }

    @Test
    void builder_ReturnsDefaultSelectMenuInteractionEvent_WhenBuildMethodCalled() {
        // Act
        TextPromptBuilder<SelectMenuInteractionEvent> actual = new TextPromptBuilder.Builder<>(clientMock, SelectMenuInteractionEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .actionRowComponent(actionRowComponentMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Assert
        String[] expectedNullFields = {
                "errorClass",
                "onErrorMessage"
        };
        assertThat(actual).hasNoNullFieldsOrPropertiesExcept(expectedNullFields);
    }

    @Test
    void builder_ReturnsDefaultButtonInteractionEvent_WhenBuildMethodCalled() {
        // Act
        TextPromptBuilder<ButtonInteractionEvent> actual = new TextPromptBuilder.Builder<>(clientMock, ButtonInteractionEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();

        // Assert
        String[] expectedNullFields = {
                "errorClass",
                "onErrorMessage",
                "actionRowComponent"
        };
        assertThat(actual).hasNoNullFieldsOrPropertiesExcept(expectedNullFields);
    }

    @Test
    void builder_ThrowsException_WhenMessageChannelIsNotSetBeforeBuild() {
        // Assert
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, MessageCreateEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("messageChannel is required");
    }

    @Test
    void builder_ThrowsException_WhenMessageCreateSpecIsNotSetBeforeBuild() {
        // Assert
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, MessageCreateEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("messageCreateSpec is required");
    }

    @Test
    void builder_ThrowsException_WhenEventProcessorIsNotSetBeforeBuild() {
        // Assert
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, MessageCreateEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventProcessor is required");
    }

    @Test
    void builder_ThrowsException_WhenEventPredicateIsNotSetBeforeBuild() {
        // Assert
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, MessageCreateEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventProcessor(event -> System.out.println("Processing..."))
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventPredicate is required");
    }

    @Test
    void builder_ThrowsException_WhenPromptTypeIsNotSetBeforeBuild() {
        // Assert
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, MessageCreateEvent.class)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventProcessor(event -> System.out.println("Processing..."))
                .eventPredicate(event -> true)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("promptType is required");
    }

    @Test
    void builder_ThrowsException_WhenSelectMenuAndPromptTypeIsDefaultButNoComponentSet() {
        // Assert
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, SelectMenuInteractionEvent.class)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventProcessor(event -> System.out.println("Processing..."))
                .eventPredicate(event -> true)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("actionRowComponent is required");
    }

    @Test
    void builder_ThrowsIllegalStateException_WhenMessageCreateEventIsNotDefaultPromptType() {
        // Arrange
        Class<MessageCreateEvent> messageCreateEventClass = MessageCreateEvent.class;

        // Assert
        PromptType promptTypeDeferReply = PromptType.DEFERRABLE_REPLY;
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, messageCreateEventClass)
                .withPromptType(promptTypeDeferReply)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("%s not supported operation for %s".formatted(promptTypeDeferReply, messageCreateEventClass));

        PromptType promptTypeDeferEdit = PromptType.DEFERRABLE_EDIT;
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, messageCreateEventClass)
                .withPromptType(promptTypeDeferEdit)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("%s not supported operation for %s".formatted(promptTypeDeferEdit, messageCreateEventClass));

        PromptType promptTypeDeleteOnResponse = PromptType.DELETE_ON_RESPONSE;
        assertThatThrownBy(() -> new TextPromptBuilder.Builder<>(clientMock, messageCreateEventClass)
                .withPromptType(promptTypeDeleteOnResponse)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("%s not supported operation for %s".formatted(promptTypeDeleteOnResponse, messageCreateEventClass));

    }
}