package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ButtonRowComponent;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TextPromptBuilderFactoryTest {

    private AutoCloseable autoCloseable;
    private TextPromptBuilderFactory underTest;
    @Mock
    private PromptFilter promptFilter;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new TextPromptBuilderFactory(promptFilter);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void defaultPrivateMessageBuilder_WithEventAndMessage() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        User userMock = mock(User.class);
        Mono<PrivateChannel> privateChannelMonoMock = Mono.just(mock(PrivateChannel.class));
        when(eventMock.getClient()).thenReturn(mock(GatewayDiscordClient.class));
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getPrivateChannel()).thenReturn(privateChannelMonoMock);
        when(promptFilter.isMessageAuthor(userMock)).thenReturn(__ -> true);

        String promptMessage = "test";

        TextPromptBuilder.Builder<MessageCreateEvent> expected = new TextPromptBuilder.Builder<>(eventMock.getClient(), MessageCreateEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(privateChannelMonoMock)
                .messageCreateSpec(MessageCreateSpec.builder().content(promptMessage).build())
                .eventPredicate(__ -> true);

        // Act
        TextPromptBuilder.Builder<MessageCreateEvent> actual = underTest.defaultPrivateMessageBuilder(eventMock, promptMessage);

        // Assert
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
                .withComparedFields("promptType", "messageChannel", "messageCreateSpec", "eventPredicate", "eventClass")
                .build();
        assertThat(actual).usingRecursiveComparison(configuration).isEqualTo(expected);
    }

    @Test
    void defaultPrivateMessageBuilder_WithEventAndMessageCreateSpec() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        User userMock = mock(User.class);
        Mono<PrivateChannel> privateChannelMonoMock = Mono.just(mock(PrivateChannel.class));
        when(eventMock.getClient()).thenReturn(mock(GatewayDiscordClient.class));
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getPrivateChannel()).thenReturn(privateChannelMonoMock);
        when(promptFilter.isMessageAuthor(userMock)).thenReturn(__ -> true);

        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder().content("test").build();

        TextPromptBuilder.Builder<MessageCreateEvent> expected = new TextPromptBuilder.Builder<>(eventMock.getClient(), MessageCreateEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(privateChannelMonoMock)
                .messageCreateSpec(messageCreateSpec)
                .eventPredicate(__ -> true);

        // Act
        TextPromptBuilder.Builder<MessageCreateEvent> actual = underTest.defaultPrivateMessageBuilder(eventMock, messageCreateSpec);

        // Assert
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
                .withComparedFields("promptType", "messageChannel", "messageCreateSpec", "eventPredicate", "eventClass")
                .build();
        assertThat(actual).usingRecursiveComparison(configuration).isEqualTo(expected);
    }

    @Test
    void deferrablePrivateButtonBuilder_WithEventMessageAndComponent() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        User userMock = mock(User.class);
        Mono<PrivateChannel> privateChannelMonoMock = Mono.just(mock(PrivateChannel.class));
        EmbedCreateSpec embedCreateSpec = mock(EmbedCreateSpec.class);
        ButtonRowComponent buttonRowComponent = mock(ButtonRowComponent.class);
        when(eventMock.getClient()).thenReturn(mock(GatewayDiscordClient.class));
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getPrivateChannel()).thenReturn(privateChannelMonoMock);
        when(promptFilter.buttonInteractionEvent(buttonRowComponent,userMock)).thenReturn(__ -> true);
        ActionRow actionRowMock = mock(ActionRow.class);
        when(buttonRowComponent.getActionRow()).thenReturn(actionRowMock);

        String promptMessage = "test";

        TextPromptBuilder.Builder<ButtonInteractionEvent> expected = new TextPromptBuilder.Builder<>(eventMock.getClient(), ButtonInteractionEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFERRABLE_REPLY)
                .messageChannel(privateChannelMonoMock)
                .actionRowComponent(buttonRowComponent)
                .messageCreateSpec(MessageCreateSpec.builder()
                        .content(promptMessage)
                        .addComponent(buttonRowComponent.getActionRow())
                        .addEmbed(embedCreateSpec)
                        .build())
                .eventPredicate(__ -> true)
                .eventProcessor(e -> {});

        // Act
        TextPromptBuilder.Builder<ButtonInteractionEvent> actual = underTest.deferrablePrivateButtonBuilder(eventMock, promptMessage, embedCreateSpec, buttonRowComponent);

        // Assert
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
                .withComparedFields("eventClass", "promptType", "messageChannel", "messageCreateSpec", "eventPredicate", "actionRowComponent")
                .build();
        assertThat(actual).usingRecursiveComparison(configuration).isEqualTo(expected);
    }

    @Test
    void deferrablePrivateButtonBuilder_WithEventMessageCreateSpecAndComponent() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        User userMock = mock(User.class);
        Mono<PrivateChannel> privateChannelMonoMock = Mono.just(mock(PrivateChannel.class));
        ButtonRowComponent buttonRowComponent = mock(ButtonRowComponent.class);
        when(eventMock.getClient()).thenReturn(mock(GatewayDiscordClient.class));
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getPrivateChannel()).thenReturn(privateChannelMonoMock);
        when(promptFilter.buttonInteractionEvent(buttonRowComponent,userMock)).thenReturn(__ -> true);
        ActionRow actionRowMock = mock(ActionRow.class);
        when(buttonRowComponent.getActionRow()).thenReturn(actionRowMock);

        MessageCreateSpec messageCreateSpec = MessageCreateSpec.builder().content("test").build();

        TextPromptBuilder.Builder<ButtonInteractionEvent> expected = new TextPromptBuilder.Builder<>(eventMock.getClient(), ButtonInteractionEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFERRABLE_REPLY)
                .messageChannel(privateChannelMonoMock)
                .actionRowComponent(buttonRowComponent)
                .messageCreateSpec(messageCreateSpec)
                .eventPredicate(__ -> true)
                .eventProcessor(e -> {});

        // Act
        TextPromptBuilder.Builder<ButtonInteractionEvent> actual = underTest.deferrablePrivateButtonBuilder(eventMock, messageCreateSpec, buttonRowComponent);

        // Assert
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
                .withComparedFields("eventClass", "promptType", "messageChannel", "messageCreateSpec", "eventPredicate", "actionRowComponent")
                .build();
        assertThat(actual).usingRecursiveComparison(configuration).isEqualTo(expected);
    }

    @Test
    void defaultPrivateSelectMenuBuilder() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        User userMock = mock(User.class);
        Mono<PrivateChannel> privateChannelMonoMock = Mono.just(mock(PrivateChannel.class));
        SelectMenuComponent selectMenuComponent = mock(SelectMenuComponent.class);
        when(eventMock.getClient()).thenReturn(mock(GatewayDiscordClient.class));
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getPrivateChannel()).thenReturn(privateChannelMonoMock);
        when(promptFilter.selectInteractionEvent(selectMenuComponent,userMock)).thenReturn(__ -> true);
        ActionRow actionRowMock = mock(ActionRow.class);
        when(selectMenuComponent.getActionRow()).thenReturn(actionRowMock);

        String promptMessage = "test";
        TextPromptBuilder.Builder<SelectMenuInteractionEvent> expected = new TextPromptBuilder.Builder<>(eventMock.getClient(), SelectMenuInteractionEvent.class)
                .withPromptType(TextPromptBuilder.PromptType.DEFAULT)
                .messageChannel(userMock.getPrivateChannel())
                .messageCreateSpec(MessageCreateSpec.builder()
                        .content(promptMessage)
                        .addComponent(selectMenuComponent.getActionRow())
                        .build())
                .actionRowComponent(selectMenuComponent)
                .eventPredicate(p__ -> true);

        // Act
        TextPromptBuilder.Builder<SelectMenuInteractionEvent> actual = underTest.defaultPrivateSelectMenuBuilder(eventMock, promptMessage, selectMenuComponent);

        // Assert
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
                .withComparedFields("eventClass", "promptType", "messageChannel", "messageCreateSpec", "eventPredicate", "actionRowComponent")
                .build();
        assertThat(actual).usingRecursiveComparison(configuration).isEqualTo(expected);
    }
}