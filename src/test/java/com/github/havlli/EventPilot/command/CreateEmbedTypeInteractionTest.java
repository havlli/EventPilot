package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import com.github.havlli.EventPilot.prompt.MessageCollector;
import com.github.havlli.EventPilot.prompt.TextPromptBuilder;
import com.github.havlli.EventPilot.prompt.TextPromptBuilderFactory;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.interaction.InteractionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CreateEmbedTypeInteractionTest {

    private AutoCloseable autoCloseable;
    private CreateEmbedTypeInteraction underTest;
    @Mock
    private ObjectFactory<CreateEmbedTypeInteraction> objectFactoryMock;
    @Mock
    private MessageCollector messageCollectorMock;
    @Mock
    private EmbedTypeService embedTypeServiceMock;
    @Mock
    private EmbedGenerator embedGeneratorMock;
    @Mock
    private TextPromptBuilderFactory promptBuilderFactoryMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CreateEmbedTypeInteraction(
                objectFactoryMock,
                messageCollectorMock,
                embedTypeServiceMock,
                embedGeneratorMock,
                promptBuilderFactoryMock
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void initiateOn() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        when(objectFactoryMock.getObject()).thenReturn(underTestSpy);

        doReturn(Mono.empty()).when(underTestSpy).startInteraction(eventMock);
        // Act
        Mono<Message> actual = underTest.initiateOn(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(underTestSpy, times(1)).startInteraction(eventMock);
    }

    @Test
    void startInteraction() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(mock(User.class));

        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        when(underTestSpy.promptName()).thenReturn(Mono.just(mock(MessageCreateEvent.class)));
        when(underTestSpy.promptImportDialog()).thenReturn(Mono.just(mock(MessageCreateEvent.class)));
        when(underTestSpy.promptConfirmation()).thenReturn(Mono.just(mock(ButtonInteractionEvent.class)));
        doReturn(Mono.empty()).when(underTestSpy).handleConfirmationResponse(any());

        // Act
        Mono<Message> actual = underTestSpy.startInteraction(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptName() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(mock(User.class));
        underTestSpy.initializeInteraction(eventMock);

        TextPromptBuilder.Builder<MessageCreateEvent> builderMock = mock(TextPromptBuilder.Builder.class);
        when(promptBuilderFactoryMock.defaultPrivateMessageBuilder(eq(eventMock), anyString())).thenReturn(builderMock);
        when(builderMock.withMessageCollector(messageCollectorMock)).thenReturn(builderMock);
        when(builderMock.eventProcessor(any())).thenReturn(builderMock);
        TextPromptBuilder<MessageCreateEvent> builder = mock(TextPromptBuilder.class);
        when(builderMock.build()).thenReturn(builder);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        when(builder.createMono()).thenReturn(Mono.just(messageCreateEventMock));


        // Act
        Mono<MessageCreateEvent> actual = underTestSpy.promptName();

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageCreateEventMock)
                .verifyComplete();
    }

    @Test
    void promptImportDialog() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(mock(User.class));
        underTestSpy.initializeInteraction(eventMock);

        TextPromptBuilder.Builder<MessageCreateEvent> builderMock = mock(TextPromptBuilder.Builder.class);
        when(promptBuilderFactoryMock.defaultPrivateMessageBuilder(eq(eventMock), anyString())).thenReturn(builderMock);
        when(builderMock.withMessageCollector(messageCollectorMock)).thenReturn(builderMock);
        when(builderMock.eventProcessor(any())).thenReturn(builderMock);
        when(builderMock.onErrorRepeat(any(), anyString())).thenReturn(builderMock);
        TextPromptBuilder<MessageCreateEvent> builder = mock(TextPromptBuilder.class);
        when(builderMock.build()).thenReturn(builder);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        when(builder.createMono()).thenReturn(Mono.just(messageCreateEventMock));


        // Act
        Mono<MessageCreateEvent> actual = underTestSpy.promptImportDialog();

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageCreateEventMock)
                .verifyComplete();
    }

    @Test
    void confirmationPrompt() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(mock(User.class));
        underTestSpy.initializeInteraction(eventMock);
        underTestSpy.setEmbedTypeBuilder(EmbedType.builder().withName("test").withStructure("test"));

        TextPromptBuilder.Builder<ButtonInteractionEvent> builderMock = mock(TextPromptBuilder.Builder.class);
        when(promptBuilderFactoryMock.deferrablePrivateButtonBuilder(
                eq(eventMock), anyString(), any(), any()
        )).thenReturn(builderMock);
        when(builderMock.withMessageCollector(messageCollectorMock)).thenReturn(builderMock);
        TextPromptBuilder<ButtonInteractionEvent> builder = mock(TextPromptBuilder.class);
        when(builderMock.build()).thenReturn(builder);
        ButtonInteractionEvent messageCreateEventMock = mock(ButtonInteractionEvent.class);
        when(builder.createMono()).thenReturn(Mono.just(messageCreateEventMock));


        // Act
        Mono<ButtonInteractionEvent> actual = underTestSpy.promptConfirmation();

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageCreateEventMock)
                .verifyComplete();
    }

    @Test
    void handleConfirmationResponse_RunsFinalizeProcess_WhenCustomIdIsConfirm() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        ButtonInteractionEvent eventMock = mock(ButtonInteractionEvent.class);
        when(eventMock.getCustomId()).thenReturn("confirm");

        InteractionResponse interactionResponse = mock(InteractionResponse.class);
        when(eventMock.getInteractionResponse()).thenReturn(interactionResponse);
        when(messageCollectorMock.cleanup()).thenReturn(Flux.empty());
        when(interactionResponse.deleteInitialResponse()).thenReturn(Mono.empty());
        when(underTestSpy.finalizeProcess()).thenReturn(Mono.just(mock(Message.class)));

        // Act
        Mono<?> actual = underTestSpy.handleConfirmationResponse(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(messageCollectorMock, times(1)).cleanup();
        verify(underTestSpy, times(1)).sendCompleteDeferredInteractionSignal(eventMock);
    }

    @Test
    void handleConfirmationResponse_DeletesAllMessagesAndTerminates_WhenCustomIdIsNotValid() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        ButtonInteractionEvent eventMock = mock(ButtonInteractionEvent.class);
        when(eventMock.getCustomId()).thenReturn("not-valid");

        InteractionResponse interactionResponse = mock(InteractionResponse.class);
        when(eventMock.getInteractionResponse()).thenReturn(interactionResponse);
        when(messageCollectorMock.cleanup()).thenReturn(Flux.empty());
        when(interactionResponse.deleteInitialResponse()).thenReturn(Mono.empty());
        when(underTestSpy.finalizeProcess()).thenReturn(Mono.just(mock(Message.class)));

        // Act
        Mono<?> actual = underTestSpy.handleConfirmationResponse(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(messageCollectorMock, times(1)).cleanup();
        verify(underTestSpy, times(1)).sendCompleteDeferredInteractionSignal(eventMock);
    }

    @Test
    void handleConfirmationResponse_DeletesAllMessagesAndRepeatsInteraction_WhenCustomIdIsRepeat() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        ButtonInteractionEvent eventMock = mock(ButtonInteractionEvent.class);
        when(eventMock.getCustomId()).thenReturn("repeat");
        ChatInputInteractionEvent initialEvent = mock(ChatInputInteractionEvent.class);
        underTestSpy.setInitialEvent(initialEvent);

        InteractionResponse interactionResponse = mock(InteractionResponse.class);
        when(eventMock.getInteractionResponse()).thenReturn(interactionResponse);
        when(messageCollectorMock.cleanup()).thenReturn(Flux.empty());
        when(interactionResponse.deleteInitialResponse()).thenReturn(Mono.empty());
        Message messageMock = mock(Message.class);
        doReturn(Mono.just(messageMock)).when(underTestSpy).startInteraction(initialEvent);

        // Act
        Mono<Message> actual = underTestSpy.handleConfirmationResponse(eventMock).cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageMock)
                .verifyComplete();
        verify(messageCollectorMock, times(1)).cleanup();
        verify(underTestSpy, times(1)).sendCompleteDeferredInteractionSignal(eventMock);
    }

    @Test
    void finalizeProcess() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        User userMock = mock(User.class);
        underTestSpy.setUser(userMock);
        underTestSpy.setEmbedTypeBuilder(EmbedType.builder().withName("test").withStructure("test"));
        PrivateChannel privateChannelMock = mock(PrivateChannel.class);
        when(userMock.getPrivateChannel()).thenReturn(Mono.just(privateChannelMock));
        Message expectedMessage = mock(Message.class);
        when(privateChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(Mono.just(expectedMessage));

        // Act
        Mono<Message> actual = underTestSpy.finalizeProcess();

        // Assert
        StepVerifier.create(actual)
                .expectNext(expectedMessage)
                .verifyComplete();
    }

    @Test
    void processImportWithException_SavesStructureToBuilder_WhenExceptionNotThrown() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        EmbedType.Builder builderSpy = spy(EmbedType.builder().withName("test"));
        underTestSpy.setEmbedTypeBuilder(builderSpy);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Message messageMock = mock(Message.class);
        when(messageCreateEventMock.getMessage()).thenReturn(messageMock);
        when(messageMock.getContent()).thenReturn("test");

        when(embedTypeServiceMock.validateJsonOrThrow(eq("test"), any())).thenReturn("json-structure");

        // Act
        underTestSpy.processImportWithException(messageCreateEventMock);

        // Assert
        verify(builderSpy, times(1)).withStructure("json-structure");
    }

    @Test
    void processImportWithException_DoesNotSavesAndThrows_WhenExceptionThrown() {
        // Arrange
        CreateEmbedTypeInteraction underTestSpy = spy(underTest);
        EmbedType.Builder builderSpy = spy(EmbedType.builder().withName("test"));
        underTestSpy.setEmbedTypeBuilder(builderSpy);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Message messageMock = mock(Message.class);
        when(messageCreateEventMock.getMessage()).thenReturn(messageMock);
        when(messageMock.getContent()).thenReturn("test");

        when(embedTypeServiceMock.validateJsonOrThrow(eq("test"), any())).thenThrow(new RuntimeException("JsonProcessingException"));

        // Assert
        assertThatThrownBy(() -> underTestSpy.processImportWithException(messageCreateEventMock))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JsonProcessingException");
        verifyNoInteractions(builderSpy);
    }
}