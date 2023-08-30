package com.github.havlli.EventPilot.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.participant.Participant;
import com.github.havlli.EventPilot.entity.participant.ParticipantService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionCallbackSpecDeferEditMono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

class EmbedInteractionGeneratorTest {

    private AutoCloseable autoCloseable;
    private EmbedInteractionGenerator underTest;
    @Mock
    private EmbedTypeService embedTypeServiceMock;
    @Mock
    private GatewayDiscordClient clientMock;
    @Mock
    private ParticipantService participantServiceMock;
    @Mock
    private EventService eventServiceMock;



    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedInteractionGenerator(embedTypeServiceMock, clientMock, participantServiceMock, eventServiceMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void subscribeInteractions_RunsSubscriptionForEachButton_WhenOneOrMoreRolesInMapDefined() throws JsonProcessingException {
        // Arrange
        EmbedGenerator embedGeneratorMock = mock(EmbedGenerator.class);
        when(embedGeneratorMock.getDelimiter()).thenReturn(",");
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(eventMock.getEventId()).thenReturn("123");
        HashMap<Integer, String> deserializedMap = new HashMap<>(Map.of(1, "one", 2, "two", 3, "three"));
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(deserializedMap);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(ButtonInteractionEvent.class)).thenReturn(Flux.empty());

        // Act
        underTest.subscribeInteractions(eventMock, embedGeneratorMock);

        // Assert
        verify(eventDispatcherMock, times(3)).on(ButtonInteractionEvent.class);
    }

    @Test
    void subscribeInteractions_RunsNoSubscriptions_WhenNoRolesInMapDefined() throws JsonProcessingException {
        // Arrange
        EmbedGenerator embedGeneratorMock = mock(EmbedGenerator.class);
        when(embedGeneratorMock.getDelimiter()).thenReturn(",");
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(eventMock.getEventId()).thenReturn("123");
        HashMap<Integer, String> deserializedMap = new HashMap<>(Map.of());
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(deserializedMap);

        EventDispatcher eventDispatcherMock = mock(EventDispatcher.class);
        when(clientMock.getEventDispatcher()).thenReturn(eventDispatcherMock);
        when(eventDispatcherMock.on(ButtonInteractionEvent.class)).thenReturn(Flux.empty());

        // Act
        underTest.subscribeInteractions(eventMock, embedGeneratorMock);

        // Assert
        verifyNoInteractions(clientMock);
    }

    @Test
    void subscribeInteractions_LogsErrorAndDoesNothing_WhenExceptionIsThrown() throws JsonProcessingException {
        // Arrange
        EmbedGenerator embedGeneratorMock = mock(EmbedGenerator.class);
        when(embedGeneratorMock.getDelimiter()).thenReturn(",");
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(eventMock.getEventId()).thenReturn("123");
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenThrow(JsonProcessingException.class);

        // Act
        underTest.subscribeInteractions(eventMock, embedGeneratorMock);

        // Assert
        verifyNoInteractions(clientMock);
    }

    @Test
    void handleEvent_UpdatesParticipantSavesEventAndReturnsMono_WhenParticipantAlreadyExist() {
        // Arrange
        Event eventMock = mock(Event.class);
        Participant participantOne = new Participant(1L, "123", "userOne", 1, 1, eventMock);
        Participant participantTwo = new Participant(2L, "234", "userTwo", 2, 1, eventMock);
        Participant participantThree = new Participant(3L, "345", "userThree", 3, 1, eventMock);
        List<Participant> participantList = List.of(participantOne, participantTwo, participantThree);
        when(eventMock.getParticipants()).thenReturn(participantList);

        ButtonInteractionEvent interactionEventMock = mock(ButtonInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(interactionEventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionEventMock.getCustomId()).thenReturn("123,1");
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(Snowflake.of("123"));

        EmbedGenerator embedGeneratorMock = mock(EmbedGenerator.class);
        when(embedGeneratorMock.getDelimiter()).thenReturn(",");
        when(embedGeneratorMock.generateEmbed(eventMock)).thenReturn(EmbedCreateSpec.builder().build());

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(interactionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);
        when(interactionCallbackSpecDeferEditMono.then(any())).thenReturn(Mono.empty());

        when(participantServiceMock.getParticipant(any(), any())).thenReturn(Optional.of(participantOne));
        // Act
        underTest.handleEvent(interactionEventMock, eventMock, embedGeneratorMock).block();

        // Assert
        verify(participantServiceMock, never()).addParticipant(any(), any());
        verify(participantServiceMock, times(1)).updateRoleIndex(any(), any());
        verify(eventServiceMock, times(1)).saveEvent(eventMock);
    }

    @Test
    void handleEvent_AddNewParticipantSavesEventAndReturnsMono_WhenSomeParticipantsAlreadyExist() {
        // Arrange
        Event eventMock = mock(Event.class);
        Participant participantOne = new Participant(1L, "123", "userOne", 1, 1, eventMock);
        Participant participantTwo = new Participant(2L, "234", "userTwo", 2, 1, eventMock);
        Participant participantThree = new Participant(3L, "345", "userThree", 3, 1, eventMock);
        List<Participant> participantList = List.of(participantOne, participantTwo, participantThree);
        when(eventMock.getParticipants()).thenReturn(participantList);

        ButtonInteractionEvent interactionEventMock = mock(ButtonInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(interactionEventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionEventMock.getCustomId()).thenReturn("123,1");
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(Snowflake.of("123"));

        EmbedGenerator embedGeneratorMock = mock(EmbedGenerator.class);
        when(embedGeneratorMock.getDelimiter()).thenReturn(",");
        when(embedGeneratorMock.generateEmbed(eventMock)).thenReturn(EmbedCreateSpec.builder().build());

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(interactionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);
        when(interactionCallbackSpecDeferEditMono.then(any())).thenReturn(Mono.empty());

        when(participantServiceMock.getParticipant(any(), any())).thenReturn(Optional.empty());
        // Act
        underTest.handleEvent(interactionEventMock, eventMock, embedGeneratorMock).block();

        // Assert
        verify(participantServiceMock, times(1)).addParticipant(any(), any());
        verify(participantServiceMock, never()).updateRoleIndex(any(), any());
        verify(eventServiceMock, times(1)).saveEvent(eventMock);
    }

    @Test
    void handleEvent_AddNewParticipantSavesEventAndReturnsMono_NoParticipantsExist() {
        // Arrange
        Event eventMock = mock(Event.class);
        List<Participant> participantList = List.of();
        when(eventMock.getParticipants()).thenReturn(participantList);

        ButtonInteractionEvent interactionEventMock = mock(ButtonInteractionEvent.class);
        Interaction interactionMock = mock(Interaction.class);
        when(interactionEventMock.getInteraction()).thenReturn(interactionMock);
        when(interactionEventMock.getCustomId()).thenReturn("123,1");
        User userMock = mock(User.class);
        when(interactionMock.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(Snowflake.of("123"));

        EmbedGenerator embedGeneratorMock = mock(EmbedGenerator.class);
        when(embedGeneratorMock.getDelimiter()).thenReturn(",");
        when(embedGeneratorMock.generateEmbed(eventMock)).thenReturn(EmbedCreateSpec.builder().build());

        InteractionCallbackSpecDeferEditMono interactionCallbackSpecDeferEditMono = mock(InteractionCallbackSpecDeferEditMono.class);
        when(interactionEventMock.deferEdit()).thenReturn(interactionCallbackSpecDeferEditMono);
        when(interactionCallbackSpecDeferEditMono.then(any())).thenReturn(Mono.empty());

        when(participantServiceMock.getParticipant(any(), any())).thenReturn(Optional.empty());
        // Act
        underTest.handleEvent(interactionEventMock, eventMock, embedGeneratorMock).block();

        // Assert
        verify(participantServiceMock, times(1)).addParticipant(any(), any());
        verify(participantServiceMock, never()).updateRoleIndex(any(), any());
        verify(eventServiceMock, times(1)).saveEvent(eventMock);
    }
}