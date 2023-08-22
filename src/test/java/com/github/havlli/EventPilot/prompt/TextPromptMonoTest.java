package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ActionRowComponent;
import com.github.havlli.EventPilot.prompt.TextPromptMono.PromptType;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TextPromptMonoTest {

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
    void mono_ReturnsMessageCreateEvent() {
        // Arrange
        Class<MessageCreateEvent> eventClass = MessageCreateEvent.class;

        Message messageMock = mock(Message.class);
        Mono<Message> messageMockMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(messageCreateSpec)).thenReturn(messageMockMono);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);

        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Flux<MessageCreateEvent> messageCreateEventFlux = Flux.just(messageCreateEventMock);
        when(eventDispatcherMock.on(eventClass)).thenReturn(messageCreateEventFlux);


        TextPromptMono<MessageCreateEvent> textPromptMono = new TextPromptMono.Builder<>(clientMock, eventClass)
                .withPromptType(PromptType.DEFAULT)
                .messageChannel(messageChannelMono)
                .messageCreateSpec(messageCreateSpec)
                .withMessageCollector(messageCollectorMock)
                .eventPredicate(event -> true)
                .eventProcessor(event -> System.out.println("event processing..."))
                .build();
        // Act
        Mono<MessageCreateEvent> actual = textPromptMono.mono();

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageCreateEventMock)
                .verifyComplete();
    }

    @Test
    void builder_ReturnsDefaultMessageCreateEvent_WhenBuildMethodCalled() {
        // Act
        TextPromptMono<MessageCreateEvent> actual = new TextPromptMono.Builder<>(clientMock, MessageCreateEvent.class)
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
        TextPromptMono<SelectMenuInteractionEvent> actual = new TextPromptMono.Builder<>(clientMock, SelectMenuInteractionEvent.class)
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
        TextPromptMono<ButtonInteractionEvent> actual = new TextPromptMono.Builder<>(clientMock, ButtonInteractionEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, MessageCreateEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, MessageCreateEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, MessageCreateEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, MessageCreateEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, MessageCreateEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, SelectMenuInteractionEvent.class)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, messageCreateEventClass)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, messageCreateEventClass)
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
        assertThatThrownBy(() -> new TextPromptMono.Builder<>(clientMock, messageCreateEventClass)
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