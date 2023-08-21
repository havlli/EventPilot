package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ActionRowComponent;
import com.github.havlli.EventPilot.prompt.TextPromptMono.PromptType;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TextPromptMonoTest {

    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);

    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void builder_ReturnsCompleteMessageCreateEvent_WhenBuildMethodCalled() {
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
    void builder_ReturnsCompleteSelectMenuInteractionEvent_WhenBuildMethodCalled() {
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);
        ActionRowComponent actionRowComponentMock = mock(ActionRowComponent.class);

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
    void builder_ReturnsCompleteButtonInteractionEvent_WhenBuildMethodCalled() {
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        // Arrange
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);

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
        GatewayDiscordClient clientMock = mock(GatewayDiscordClient.class);
        MessageChannel messageChannelMock = mock(MessageChannel.class);
        Mono<MessageChannel> messageChannelMono = Mono.just(messageChannelMock);
        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder()
                .content("Test")
                .build();
        MessageCollector messageCollectorMock = mock(MessageCollector.class);
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