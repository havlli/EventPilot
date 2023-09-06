package com.github.havlli.EventPilot.command.createevent;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.entity.guild.GuildService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import com.github.havlli.EventPilot.prompt.*;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
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
import java.util.function.Predicate;

import static org.mockito.Mockito.*;

class CreateEventInteractionTest {

    private AutoCloseable autoCloseable;
    private CreateEventInteraction underTest;
    @Mock
    private GatewayDiscordClient clientMock;
    @Mock
    private MessageCollector collectorMock;
    @Mock
    private PromptFormatter formatterMock;
    @Mock
    private PromptFilter filterMock;
    @Mock
    private PromptService promptServiceMock;
    @Mock
    private EmbedGenerator embedGeneratorMock;
    @Mock
    private EventService eventServiceMock;
    @Mock
    private GuildService guildServiceMock;
    @Mock
    private EmbedTypeService embedTypeServiceMock;
    @Mock
    private TimeService timeServiceMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CreateEventInteraction(
                clientMock,
                collectorMock,
                formatterMock,
                filterMock,
                promptServiceMock,
                embedGeneratorMock,
                eventServiceMock,
                guildServiceMock,
                embedTypeServiceMock,
                timeServiceMock
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void start_CallsFinalizeProcessAndDeletesInitialInteraction_WhenCustomIdIsConfirm() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        PrivateChannel privateChannelMock = mock(PrivateChannel.class);
        when(userMock.getPrivateChannel()).thenReturn(Mono.just(privateChannelMock));

        Snowflake guildId = Snowflake.of(123);
        when(promptServiceMock.fetchGuildId(eventMock)).thenReturn(guildId);
        Guild guildMock = mock(Guild.class);
        when(guildServiceMock.getGuildById(guildId.asString())).thenReturn(guildMock);
        when(userMock.getUsername()).thenReturn("username");
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(embedTypeServiceMock.getEmbedTypeById(1)).thenReturn(embedTypeMock);

        CreateEventInteraction underTestSpy = spy(underTest);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Mono<MessageCreateEvent> messageCreateEventMono = Mono.just(messageCreateEventMock);
        doReturn(messageCreateEventMono).when(underTestSpy).promptName();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDescription();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDateTime();

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Mono<SelectMenuInteractionEvent> selectMenuInteractionEventMono = Mono.just(selectMenuInteractionEventMock);
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptEmbedType();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptRaidSelect();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptMemberSize();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptDestinationChannel();

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Mono<ButtonInteractionEvent> buttonInteractionEventMono = Mono.just(buttonInteractionEventMock);
        doReturn(buttonInteractionEventMono).when(underTestSpy).promptConfirmationAndDeferReply();
        when(buttonInteractionEventMock.getCustomId()).thenReturn("confirm");

        doReturn(Mono.empty()).when(underTestSpy).finalizeProcess();
        InteractionResponse interactionResponseMock = mock(InteractionResponse.class);
        when(buttonInteractionEventMock.getInteractionResponse()).thenReturn(interactionResponseMock);
        when(interactionResponseMock.deleteInitialResponse()).thenReturn(Mono.empty());

        // Act
        Mono<Message> actual = underTestSpy.start(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void start_CallsMessageCleanupAndStartsOver_WhenCustomIdIsRepeat() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        PrivateChannel privateChannelMock = mock(PrivateChannel.class);
        when(userMock.getPrivateChannel()).thenReturn(Mono.just(privateChannelMock));

        Snowflake guildId = Snowflake.of(123);
        when(promptServiceMock.fetchGuildId(eventMock)).thenReturn(guildId);
        Guild guildMock = mock(Guild.class);
        when(guildServiceMock.getGuildById(guildId.asString())).thenReturn(guildMock);
        when(userMock.getUsername()).thenReturn("username");
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(embedTypeServiceMock.getEmbedTypeById(1)).thenReturn(embedTypeMock);

        CreateEventInteraction underTestSpy = spy(underTest);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Mono<MessageCreateEvent> messageCreateEventMono = Mono.just(messageCreateEventMock);
        doReturn(messageCreateEventMono).when(underTestSpy).promptName();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDescription();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDateTime();

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Mono<SelectMenuInteractionEvent> selectMenuInteractionEventMono = Mono.just(selectMenuInteractionEventMock);
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptEmbedType();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptRaidSelect();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptMemberSize();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptDestinationChannel();

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Mono<ButtonInteractionEvent> buttonInteractionEventMono = Mono.just(buttonInteractionEventMock);
        doReturn(buttonInteractionEventMono).when(underTestSpy).promptConfirmationAndDeferReply();
        when(buttonInteractionEventMock.getCustomId()).thenReturn("repeat");

        when(collectorMock.cleanup()).thenReturn(Flux.empty());
        InteractionResponse interactionResponseMock = mock(InteractionResponse.class);
        when(buttonInteractionEventMock.getInteractionResponse()).thenReturn(interactionResponseMock);
        when(interactionResponseMock.deleteInitialResponse()).thenReturn(Mono.empty());

        when(underTestSpy.start(eventMock)).thenCallRealMethod().thenReturn(Mono.empty());

        // Act
        Mono<Message> actual = underTestSpy.start(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(collectorMock, only()).cleanup();
    }

    @Test
    void start_CallsMessageCleanupAndDeletesInitialInteraction_WhenCustomIdIsNotMatching() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        PrivateChannel privateChannelMock = mock(PrivateChannel.class);
        when(userMock.getPrivateChannel()).thenReturn(Mono.just(privateChannelMock));

        Snowflake guildId = Snowflake.of(123);
        when(promptServiceMock.fetchGuildId(eventMock)).thenReturn(guildId);
        Guild guildMock = mock(Guild.class);
        when(guildServiceMock.getGuildById(guildId.asString())).thenReturn(guildMock);
        when(userMock.getUsername()).thenReturn("username");
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(embedTypeServiceMock.getEmbedTypeById(1)).thenReturn(embedTypeMock);

        CreateEventInteraction underTestSpy = spy(underTest);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Mono<MessageCreateEvent> messageCreateEventMono = Mono.just(messageCreateEventMock);
        doReturn(messageCreateEventMono).when(underTestSpy).promptName();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDescription();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDateTime();

        SelectMenuInteractionEvent selectMenuInteractionEventMock = mock(SelectMenuInteractionEvent.class);
        Mono<SelectMenuInteractionEvent> selectMenuInteractionEventMono = Mono.just(selectMenuInteractionEventMock);
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptEmbedType();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptRaidSelect();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptMemberSize();
        doReturn(selectMenuInteractionEventMono).when(underTestSpy).promptDestinationChannel();

        ButtonInteractionEvent buttonInteractionEventMock = mock(ButtonInteractionEvent.class);
        Mono<ButtonInteractionEvent> buttonInteractionEventMono = Mono.just(buttonInteractionEventMock);
        doReturn(buttonInteractionEventMono).when(underTestSpy).promptConfirmationAndDeferReply();
        when(buttonInteractionEventMock.getCustomId()).thenReturn("default");

        when(collectorMock.cleanup()).thenReturn(Flux.empty());
        InteractionResponse interactionResponseMock = mock(InteractionResponse.class);
        when(buttonInteractionEventMock.getInteractionResponse()).thenReturn(interactionResponseMock);
        when(interactionResponseMock.deleteInitialResponse()).thenReturn(Mono.empty());

        // Act
        Mono<Message> actual = underTestSpy.start(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(collectorMock, only()).cleanup();
    }

    @Test
    void start_PropagatesErrorsDownstream() {
        // Arrange
        ChatInputInteractionEvent eventMock = mock(ChatInputInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(eventMock.getInteraction()).thenReturn(interactionMock);
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        PrivateChannel privateChannelMock = mock(PrivateChannel.class);
        when(userMock.getPrivateChannel()).thenReturn(Mono.just(privateChannelMock));

        Snowflake guildId = Snowflake.of(123);
        when(promptServiceMock.fetchGuildId(eventMock)).thenReturn(guildId);
        Guild guildMock = mock(Guild.class);
        when(guildServiceMock.getGuildById(guildId.asString())).thenReturn(guildMock);
        when(userMock.getUsername()).thenReturn("username");
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(embedTypeServiceMock.getEmbedTypeById(1)).thenReturn(embedTypeMock);

        CreateEventInteraction underTestSpy = spy(underTest);
        MessageCreateEvent messageCreateEventMock = mock(MessageCreateEvent.class);
        Mono<MessageCreateEvent> messageCreateEventMono = Mono.just(messageCreateEventMock);
        doReturn(messageCreateEventMono).when(underTestSpy).promptName();
        doReturn(messageCreateEventMono).when(underTestSpy).promptDescription();
        doReturn(Mono.error(RuntimeException::new)).when(underTestSpy).promptDateTime();

        // Act
        Mono<Message> actual = underTestSpy.start(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void promptName() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(MessageCreateEvent.class)).thenReturn(Flux.empty());

        Predicate<MessageCreateEvent> predicate = event -> false;
        when(filterMock.isMessageAuthor(any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<MessageCreateEvent> actual = underTest.promptName();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptDescription() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(MessageCreateEvent.class)).thenReturn(Flux.empty());

        Predicate<MessageCreateEvent> predicate = event -> false;
        when(filterMock.isMessageAuthor(any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<MessageCreateEvent> actual = underTest.promptDescription();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptDateTime() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(MessageCreateEvent.class)).thenReturn(Flux.empty());

        Predicate<MessageCreateEvent> predicate = event -> false;
        when(filterMock.isMessageAuthor(any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<MessageCreateEvent> actual = underTest.promptDateTime();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptEmbedType() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(SelectMenuInteractionEvent.class)).thenReturn(Flux.empty());

        Predicate<SelectMenuInteractionEvent> predicate = event -> false;
        when(filterMock.selectInteractionEvent(any(), any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<SelectMenuInteractionEvent> actual = underTest.promptEmbedType();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptRaidSelect() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(SelectMenuInteractionEvent.class)).thenReturn(Flux.empty());

        Predicate<SelectMenuInteractionEvent> predicate = event -> false;
        when(filterMock.selectInteractionEvent(any(), any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<SelectMenuInteractionEvent> actual = underTest.promptRaidSelect();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptMemberSize() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(SelectMenuInteractionEvent.class)).thenReturn(Flux.empty());

        Predicate<SelectMenuInteractionEvent> predicate = event -> false;
        when(filterMock.selectInteractionEvent(any(), any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<SelectMenuInteractionEvent> actual = underTest.promptMemberSize();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptDestinationChannel() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(SelectMenuInteractionEvent.class)).thenReturn(Flux.empty());

        Predicate<SelectMenuInteractionEvent> predicate = event -> false;
        when(filterMock.selectInteractionEvent(any(), any())).thenReturn(predicate);

        ChatInputInteractionEvent initialEventMock = mock(ChatInputInteractionEvent.class);
        underTest.setInitialEvent(initialEventMock);
        Interaction interactionMock = mock(Interaction.class);
        when(initialEventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionMock.getChannelId()).thenReturn(Snowflake.of("12345"));

        TextChannel textChannelMock = mock(TextChannel.class);
        when(textChannelMock.getName()).thenReturn("textChannel");
        when(textChannelMock.getId()).thenReturn(Snowflake.of("123456"));
        when(promptServiceMock.fetchGuildTextChannels(initialEventMock)).thenReturn(Flux.just(textChannelMock));


        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<SelectMenuInteractionEvent> actual = underTest.promptDestinationChannel();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void promptConfirmationAndDeferReply() {
        // Arrange
        PrivateChannel messageChannelMock = mock(PrivateChannel.class);
        Mono<PrivateChannel> messageChannelMono = Mono.just(messageChannelMock);

        Message messageMock = mock(Message.class);
        Mono<Message> messageMono = Mono.just(messageMock);
        when(messageChannelMock.createMessage(any(MessageCreateSpec.class))).thenReturn(messageMono);
        doNothing().when(collectorMock).collect(messageMock);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(ButtonInteractionEvent.class)).thenReturn(Flux.empty());

        Predicate<ButtonInteractionEvent> predicate = event -> false;
        when(filterMock.buttonInteractionEvent(any(), any())).thenReturn(predicate);

        // Act
        underTest.setPrivateChannel(messageChannelMono);
        Mono<ButtonInteractionEvent> actual = underTest.promptConfirmationAndDeferReply();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void finalizeProcess() {
        // Arrange
        Event.Builder builderMock = mock(Event.Builder.class);
        underTest.setEventBuilder(builderMock);
        when(builderMock.getDestinationChannelId()).thenReturn("1234");

        ChatInputInteractionEvent initialEvent = mock(ChatInputInteractionEvent.class);
        underTest.setInitialEvent(initialEvent);

        Interaction interaction = mock(Interaction.class);
        when(initialEvent.getInteraction()).thenReturn(interaction);

        discord4j.core.object.entity.Guild guildMock = mock(discord4j.core.object.entity.Guild.class);
        Mono<discord4j.core.object.entity.Guild> guildMono = Mono.just(guildMock);
        when(interaction.getGuild()).thenReturn(guildMono);
        Mono<GuildChannel> guildChannelMono = mock(Mono.class);
        when(guildMock.getChannelById(Snowflake.of("1234"))).thenReturn(guildChannelMono);
        Mono<MessageChannel> messageChannelMono = mock(Mono.class);
        when(guildChannelMono.cast(MessageChannel.class)).thenReturn(messageChannelMono);

        Message messageMock = mock(Message.class);
        when(messageChannelMono.flatMap(any())).thenReturn(Mono.just(messageMock));
        when(messageMock.getId()).thenReturn(Snowflake.of("12345"));
        Event eventMock = mock(Event.class);
        when(builderMock.build()).thenReturn(eventMock);

        when(formatterMock.messageUrl(any(), any(), any())).thenReturn("messageUrl");
        Mono<PrivateChannel> privateChannelMono = mock(Mono.class);
        underTest.setPrivateChannel(privateChannelMono);
        when(privateChannelMono.flatMap(any())).thenReturn(Mono.empty());

        when(embedGeneratorMock.generateEmbed(eventMock)).thenReturn(EmbedCreateSpec.builder().build());
        List<LayoutComponent> layoutComponents = List.of();
        when(embedGeneratorMock.generateComponents(eventMock)).thenReturn(layoutComponents);

        doNothing().when(embedGeneratorMock).subscribeInteractions(eventMock);
        doNothing().when(eventServiceMock).saveEvent(eventMock);

        when(messageMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.empty());

        // Act
        Mono<Message> actual = underTest.finalizeProcess();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }
}