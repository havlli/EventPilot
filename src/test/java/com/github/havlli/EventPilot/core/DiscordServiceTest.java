package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.component.CustomComponentFactory;
import com.github.havlli.EventPilot.component.selectmenu.ExpiredSelectMenu;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.http.client.ClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiscordServiceTest {

    private AutoCloseable autoCloseable;
    private DiscordService underTest;
    @Mock
    private GatewayDiscordClient clientMock;
    @Mock
    private EventService eventServiceMock;
    @Mock
    private CustomComponentFactory componentFactory;
    @Mock
    private EmbedGenerator embedGeneratorMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new DiscordService(clientMock, eventServiceMock, componentFactory, embedGeneratorMock);
        when(componentFactory.getDefaultSelectMenu(CustomComponentFactory.SelectMenuType.EXPIRED_SELECT_MENU))
                .thenReturn(new ExpiredSelectMenu());
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void updateEventMessage_ReturnsMessageMono_WhenMessageExists() {
        // Arrange
        Event eventMock = mock(Event.class);
        when(eventMock.getDestinationChannelId()).thenReturn("123456789");
        when(eventMock.getEventId()).thenReturn("12345");
        Message messageMock = mock(Message.class);
        when(clientMock.getMessageById(any(), any())).thenReturn(Mono.just(messageMock));
        when(messageMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.empty());
        EmbedCreateSpec embedCreateSpecMock = mock(EmbedCreateSpec.class);
        when(embedGeneratorMock.generateEmbed(eventMock)).thenReturn(embedCreateSpecMock);

        // Act
        Mono<Message> actual = underTest.updateEventMessage(eventMock);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void deactivateEvents_ReturnsEmptyFlux_WhenMessageAlreadyDeactivated() {
        // Arrange
        Event eventOneMock = mock(Event.class);
        String eventOneId = "12345";
        when(eventOneMock.getEventId()).thenReturn(eventOneId);
        String eventOneChannel = "123456789";
        when(eventOneMock.getDestinationChannelId()).thenReturn(eventOneChannel);

        Message messageOneMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        when(messageOneMock.getComponents()).thenReturn(List.of(layoutComponentMock));
        MessageComponent messageComponentMock = mock(MessageComponent.class);
        when(layoutComponentMock.getChildren()).thenReturn(List.of(messageComponentMock));
        ComponentData componentDataMock = mock(ComponentData.class);
        when(messageComponentMock.getData()).thenReturn(componentDataMock);
        when(componentDataMock.customId()).thenReturn(Possible.of("expired"));

        when(clientMock.getMessageById(Snowflake.of(eventOneChannel), Snowflake.of(eventOneId)))
                .thenReturn(Mono.just(messageOneMock));

        // Act
        Flux<Message> actual = underTest.deactivateEvents(List.of(eventOneMock));

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(eventServiceMock);
    }

    @Test
    void deactivateEvents_ReturnsOneMessageInFlux_WhenOneMessageToDeactivate() {
        // Arrange
        Event eventOneMock = mock(Event.class);
        String eventOneId = "12345";
        when(eventOneMock.getEventId()).thenReturn(eventOneId);
        String eventOneChannel = "123456789";
        when(eventOneMock.getDestinationChannelId()).thenReturn(eventOneChannel);

        Message messageOneMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        when(messageOneMock.getComponents()).thenReturn(List.of(layoutComponentMock));
        MessageComponent messageComponentMock = mock(MessageComponent.class);
        when(layoutComponentMock.getChildren()).thenReturn(List.of(messageComponentMock));
        ComponentData componentDataMock = mock(ComponentData.class);
        when(messageComponentMock.getData()).thenReturn(componentDataMock);
        when(componentDataMock.customId()).thenReturn(Possible.of("not-expired"));

        when(messageOneMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.just(messageOneMock));

        when(clientMock.getMessageById(Snowflake.of(eventOneChannel), Snowflake.of(eventOneId)))
                .thenReturn(Mono.just(messageOneMock));
        when(messageOneMock.getId()).thenReturn(Snowflake.of(1234L));

        // Act
        Flux<Message> actual = underTest.deactivateEvents(List.of(eventOneMock));

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNext(messageOneMock)
                .verifyComplete();
        verify(eventServiceMock, times(1)).deleteEventById(anyString());
    }

    @Test
    void deactivateEvents_ReturnsMultipleMessagesInFlux_WhenMultipleMessagesToDeactivate() {
        // Arrange
        Event eventOneMock = mock(Event.class);
        String eventOneId = "12345";
        when(eventOneMock.getEventId()).thenReturn(eventOneId);
        String eventOneChannel = "123456789";
        when(eventOneMock.getDestinationChannelId()).thenReturn(eventOneChannel);

        Event eventTwoMock = mock(Event.class);
        String eventTwoId = "212345";
        when(eventTwoMock.getEventId()).thenReturn(eventTwoId);
        String eventTwoChannel = "2123456789";
        when(eventTwoMock.getDestinationChannelId()).thenReturn(eventTwoChannel);

        Message messageOneMock = mock(Message.class);
        Message messageTwoMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        when(messageOneMock.getComponents()).thenReturn(List.of(layoutComponentMock));
        when(messageTwoMock.getComponents()).thenReturn(List.of(layoutComponentMock));
        MessageComponent messageComponentMock = mock(MessageComponent.class);
        when(layoutComponentMock.getChildren()).thenReturn(List.of(messageComponentMock));
        ComponentData componentDataMock = mock(ComponentData.class);
        when(messageComponentMock.getData()).thenReturn(componentDataMock);
        when(componentDataMock.customId()).thenReturn(Possible.of("not-expired"));

        when(messageOneMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.just(messageOneMock));
        when(messageTwoMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.just(messageTwoMock));

        when(clientMock.getMessageById(Snowflake.of(eventOneChannel), Snowflake.of(eventOneId)))
                .thenReturn(Mono.just(messageOneMock));
        when(clientMock.getMessageById(Snowflake.of(eventTwoChannel), Snowflake.of(eventTwoId)))
                .thenReturn(Mono.just(messageTwoMock));
        when(messageOneMock.getId()).thenReturn(Snowflake.of(1234L));
        when(messageTwoMock.getId()).thenReturn(Snowflake.of(2345L));

        // Act
        Flux<Message> actual = underTest.deactivateEvents(List.of(eventOneMock, eventTwoMock));

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNext(messageOneMock, messageTwoMock)
                .verifyComplete();
        verify(eventServiceMock, times(2)).deleteEventById(anyString());
    }

    @Test
    void deactivateEvents_ReturnsCorrectMessagesInFlux_WhenMultipleStatesOfDeactivate() {
        // Arrange
        Event eventOneMock = mock(Event.class);
        String eventOneId = "12345";
        when(eventOneMock.getEventId()).thenReturn(eventOneId);
        String eventOneChannel = "123456789";
        when(eventOneMock.getDestinationChannelId()).thenReturn(eventOneChannel);

        Event eventTwoMock = mock(Event.class);
        String eventTwoId = "212345";
        when(eventTwoMock.getEventId()).thenReturn(eventTwoId);
        String eventTwoChannel = "2123456789";
        when(eventTwoMock.getDestinationChannelId()).thenReturn(eventTwoChannel);

        Message messageOneMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        when(messageOneMock.getComponents()).thenReturn(List.of(layoutComponentMock));
        MessageComponent messageComponentMock = mock(MessageComponent.class);
        when(layoutComponentMock.getChildren()).thenReturn(List.of(messageComponentMock));
        ComponentData componentDataMock = mock(ComponentData.class);
        when(messageComponentMock.getData()).thenReturn(componentDataMock);
        when(componentDataMock.customId()).thenReturn(Possible.of("not-expired"));

        Message messageTwoMock = mock(Message.class);
        LayoutComponent layoutComponentSecondMock = mock(LayoutComponent.class);
        when(messageTwoMock.getComponents()).thenReturn(List.of(layoutComponentSecondMock));
        MessageComponent messageComponentSecondMock = mock(MessageComponent.class);
        when(layoutComponentSecondMock.getChildren()).thenReturn(List.of(messageComponentSecondMock));
        ComponentData componentDataSecondMock = mock(ComponentData.class);
        when(messageComponentSecondMock.getData()).thenReturn(componentDataSecondMock);
        when(componentDataSecondMock.customId()).thenReturn(Possible.of("expired"));

        when(messageOneMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.just(messageOneMock));
        when(messageTwoMock.edit(any(MessageEditSpec.class))).thenReturn(Mono.just(messageTwoMock));

        when(clientMock.getMessageById(Snowflake.of(eventOneChannel), Snowflake.of(eventOneId)))
                .thenReturn(Mono.just(messageOneMock));
        when(clientMock.getMessageById(Snowflake.of(eventTwoChannel), Snowflake.of(eventTwoId)))
                .thenReturn(Mono.just(messageTwoMock));
        when(messageOneMock.getId()).thenReturn(Snowflake.of(1234L));
        when(messageTwoMock.getId()).thenReturn(Snowflake.of(2345L));

        // Act
        Flux<Message> actual = underTest.deactivateEvents(List.of(eventOneMock, eventTwoMock));

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNext(messageOneMock)
                .verifyComplete();
        verify(eventServiceMock, times(1)).deleteEventById(anyString());
    }

    @Test
    void deactivateEvents_HandlesErrorGracefully_WhenMessageNotFound() {
        // Arrange
        Event eventOneMock = mock(Event.class);
        String eventOneId = "12345";
        when(eventOneMock.getEventId()).thenReturn(eventOneId);
        String eventOneChannel = "123456789";
        when(eventOneMock.getDestinationChannelId()).thenReturn(eventOneChannel);

        Message messageOneMock = mock(Message.class);
        LayoutComponent layoutComponentMock = mock(LayoutComponent.class);
        when(messageOneMock.getComponents()).thenReturn(List.of(layoutComponentMock));
        MessageComponent messageComponentMock = mock(MessageComponent.class);
        when(layoutComponentMock.getChildren()).thenReturn(List.of(messageComponentMock));
        ComponentData componentDataMock = mock(ComponentData.class);
        when(messageComponentMock.getData()).thenReturn(componentDataMock);
        when(componentDataMock.customId()).thenReturn(Possible.of("not-expired"));

        when(messageOneMock.edit(any(MessageEditSpec.class))).thenThrow(ClientException.class);

        when(clientMock.getMessageById(Snowflake.of(eventOneChannel), Snowflake.of(eventOneId)))
                .thenReturn(Mono.just(messageOneMock));
        when(messageOneMock.getId()).thenReturn(Snowflake.of(1234L));

        // Act
        DiscordService discordService = spy(new DiscordService(clientMock, eventServiceMock, componentFactory, embedGeneratorMock));
        Flux<Message> actual = discordService.deactivateEvents(List.of(eventOneMock));

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(eventServiceMock);
    }
}