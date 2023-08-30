package com.github.havlli.EventPilot.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeService;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.participant.Participant;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmbedGeneratorTest {

    private AutoCloseable autoCloseable;
    private EmbedGenerator underTest;
    @Mock
    private EmbedPreviewable embedPreviewableMock;
    @Mock
    private EmbedFormatter embedFormatterMock;
    @Mock
    private ComponentGenerator componentGeneratorMock;
    @Mock
    private EmbedTypeService embedTypeServiceMock;
    @Mock
    private EmbedInteractionGenerator interactionGeneratorMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EmbedGenerator(
                embedFormatterMock,
                componentGeneratorMock,
                embedTypeServiceMock,
                interactionGeneratorMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void generatePreview_ReturnsCorrectList_WhenMultipleFieldsAlreadySet() {
        // Arrange
        Event.Builder builder = Event.builder()
                .withName("test")
                .withDescription("test")
                .withDestinationChannel("testId")
                .withDateTime(Instant.now());
        when(embedPreviewableMock.previewFields()).thenReturn(builder.previewFields());

        // Act
        EmbedCreateSpec embedCreateSpec = underTest.generatePreview(embedPreviewableMock);

        // Assert
        assertThat(embedCreateSpec.fields()).hasSize(4);
    }

    @Test
    void generatePreview_ReturnsValidEmbedCreateSpec_WhenNoFieldsAreSet() {
        // Arrange
        Event.Builder builder = Event.builder();
        when(embedPreviewableMock.previewFields()).thenReturn(builder.previewFields());

        // Act
        EmbedCreateSpec embedCreateSpec = underTest.generatePreview(embedPreviewableMock);

        // Assert
        assertThat(embedCreateSpec.fields()).hasSize(0);
    }

    @Test
    void getPopulatedFields_ReturnsListOfGeneratedFields_WhenEventGotParticipants() throws JsonProcessingException {
        // Arrange
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Absence",
                -2, "Late",
                -3, "Tentative",
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(fieldsMap);

        Participant participantOne = new Participant(1L,"1","userOne",1,1, eventMock);
        Participant participantTwo = new Participant(2L,"2","userTwo",2,1, eventMock);
        Participant participantThree = new Participant(3L,"3","userThree",3,5, eventMock);
        Participant participantFour = new Participant(4L,"4","userFour",4,-3, eventMock);
        Participant participantFive = new Participant(5L,"5","userFive",5,-1, eventMock);
        List<Participant> participantList = List.of(participantOne, participantTwo, participantThree, participantFour, participantFive);

        when(eventMock.getParticipants()).thenReturn(participantList);
        when(embedFormatterMock.createConcatField(anyString(),anyList(),anyBoolean())).thenReturn("concatField");

        // Act
        List<EmbedCreateFields.Field> actualFields = underTest.constructPopulatedFields(eventMock);

        // Assert
        assertThat(actualFields).hasSize(4);
    }

    @Test
    void getPopulatedFields_ReturnsEmptyListOfGeneratedFields_WhenEventGotNoParticipants() throws JsonProcessingException {
        // Arrange
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Absence",
                -2, "Late",
                -3, "Tentative",
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(fieldsMap);

        List<Participant> participantList = List.of();

        when(eventMock.getParticipants()).thenReturn(participantList);
        when(embedFormatterMock.createConcatField(anyString(),anyList(),anyBoolean())).thenReturn("concatField");

        // Act
        List<EmbedCreateFields.Field> actualFields = underTest.constructPopulatedFields(eventMock);

        // Assert
        assertThat(actualFields).hasSize(0);
    }

    @Test
    void getPopulatedFields_ReturnsEmptyListAndLogsJsonProcessingException_WhenExceptionThrown() throws JsonProcessingException {
        // Arrange
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenThrow(JsonProcessingException.class);

       // Act
        List<EmbedCreateFields.Field> actualFields = underTest.constructPopulatedFields(eventMock);

        // Assert
        assertThat(actualFields).hasSize(0);
    }

    @Test
    void generateEmbed_ReturnsEmbedCreateSpec_WhenEventHasMultipleParticipants() throws JsonProcessingException {
        // Arrange
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Absence",
                -2, "Late",
                -3, "Tentative",
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(eventMock.getName()).thenReturn("name");
        when(eventMock.getDescription()).thenReturn("description");
        when(eventMock.getMemberSize()).thenReturn("25");
        when(embedFormatterMock.leaderWithId(any(), any())).thenReturn("formatted leader");
        when(embedFormatterMock.date(eventMock.getDateTime())).thenReturn("date");
        when(embedFormatterMock.time(eventMock.getDateTime())).thenReturn("time");
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(fieldsMap);

        Participant participantOne = new Participant(1L,"1","userOne",1,1, eventMock);
        Participant participantTwo = new Participant(2L,"2","userTwo",2,1, eventMock);
        Participant participantThree = new Participant(3L,"3","userThree",3,5, eventMock);
        Participant participantFour = new Participant(4L,"4","userFour",4,-3, eventMock);
        Participant participantFive = new Participant(5L,"5","userFive",5,-1, eventMock);
        List<Participant> participantList = List.of(participantOne, participantTwo, participantThree, participantFour, participantFive);

        when(embedFormatterMock.raidSize(anyInt(), anyString())).thenReturn("formatted raid size");

        when(eventMock.getParticipants()).thenReturn(participantList);
        when(embedFormatterMock.createConcatField(anyString(),anyList(),anyBoolean())).thenReturn("concatField");

        // Act
        EmbedCreateSpec actual = underTest.generateEmbed(eventMock);

        // Assert
        assertThat(actual.fields()).hasSize(10);
    }

    @Test
    void generateEmbed_ReturnsEmbedCreateSpec_WhenEventHasOneParticipant() throws JsonProcessingException {
        // Arrange
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Absence",
                -2, "Late",
                -3, "Tentative",
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(eventMock.getName()).thenReturn("name");
        when(eventMock.getDescription()).thenReturn("description");
        when(eventMock.getMemberSize()).thenReturn("25");
        when(embedFormatterMock.leaderWithId(any(), any())).thenReturn("formatted leader");
        when(embedFormatterMock.date(eventMock.getDateTime())).thenReturn("date");
        when(embedFormatterMock.time(eventMock.getDateTime())).thenReturn("time");
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(fieldsMap);

        Participant participantOne = new Participant(1L,"1","userOne",1,1, eventMock);
        List<Participant> participantList = List.of(participantOne);

        when(embedFormatterMock.raidSize(anyInt(), anyString())).thenReturn("formatted raid size");

        when(eventMock.getParticipants()).thenReturn(participantList);
        when(embedFormatterMock.createConcatField(anyString(),anyList(),anyBoolean())).thenReturn("concatField");

        // Act
        EmbedCreateSpec actual = underTest.generateEmbed(eventMock);

        // Assert
        assertThat(actual.fields()).hasSize(7);
    }

    @Test
    void generateEmbed_ReturnsEmbedCreateSpec_WhenEventHasNoParticipants() throws JsonProcessingException {
        // Arrange
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Absence",
                -2, "Late",
                -3, "Tentative",
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        when(eventMock.getName()).thenReturn("name");
        when(eventMock.getDescription()).thenReturn("description");
        when(eventMock.getMemberSize()).thenReturn("25");
        when(embedFormatterMock.leaderWithId(any(), any())).thenReturn("formatted leader");
        when(embedFormatterMock.date(eventMock.getDateTime())).thenReturn("date");
        when(embedFormatterMock.time(eventMock.getDateTime())).thenReturn("time");
        when(embedTypeServiceMock.getDeserializedMap(embedTypeMock)).thenReturn(fieldsMap);

        List<Participant> participantList = List.of();

        when(embedFormatterMock.raidSize(anyInt(), anyString())).thenReturn("formatted raid size");

        when(eventMock.getParticipants()).thenReturn(participantList);
        when(embedFormatterMock.createConcatField(anyString(),anyList(),anyBoolean())).thenReturn("concatField");

        // Act
        EmbedCreateSpec actual = underTest.generateEmbed(eventMock);

        // Assert
        assertThat(actual.fields()).hasSize(6);
    }

    @Test
    void subscribeInteractions_CallsInteractionGenerator() {
        // Arrange
        Event eventMock = mock(Event.class);

        // Act
        underTest.subscribeInteractions(eventMock);

        // Assert
        verify(interactionGeneratorMock, only()).subscribeInteractions(eventMock, underTest);
    }

    @Test
    void generateComponents_CallsComponentGenerator() {
        // Arrange
        Event eventMock = mock(Event.class);

        // Act
        underTest.generateComponents(eventMock);

        // Assert
        verify(componentGeneratorMock, only()).eventButtons(any(), eq(eventMock));
    }
}