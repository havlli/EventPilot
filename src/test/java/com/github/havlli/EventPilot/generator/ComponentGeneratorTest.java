package com.github.havlli.EventPilot.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.embedtype.EmbedTypeSerialization;
import com.github.havlli.EventPilot.entity.event.Event;
import discord4j.core.object.component.LayoutComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentGeneratorTest {

    private ComponentGenerator underTest;
    private AutoCloseable autoCloseable;
    @Mock
    private EmbedTypeSerialization serializationMock;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new ComponentGenerator(serializationMock);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void eventButtons_ReturnsListOfLayoutComponents_WhenFieldMapContainsDefaultAndRoleFieldKeys() throws JsonProcessingException {
        // Arrange
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        String mapStructure = "mapStructure";
        when(embedTypeMock.getStructure()).thenReturn(mapStructure);
        String id = "123456";
        when(eventMock.getEventId()).thenReturn(id);
        String delimiter = ",";
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
        when(serializationMock.deserializeMap(mapStructure)).thenReturn(fieldsMap);

        List<String> expectedCustomIds = fieldsMap.keySet()
                .stream()
                .map(key -> id + delimiter + key)
                .toList();

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, eventMock);

        // Assert
        List<String> actualCustomIds = actual.stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().get())
                .toList();

        assertThat(actualCustomIds).containsExactlyInAnyOrderElementsOf(expectedCustomIds);
    }

    @Test
    void eventButtons_ReturnsListOfLayoutComponents_WhenFieldMapContainsOnlyRoleFieldKeys() throws JsonProcessingException {
        // Arrange
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        String mapStructure = "mapStructure";
        when(embedTypeMock.getStructure()).thenReturn(mapStructure);
        String id = "123456";
        when(eventMock.getEventId()).thenReturn(id);
        String delimiter = ",";
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        when(serializationMock.deserializeMap(mapStructure)).thenReturn(fieldsMap);
        List<String> expectedCustomIds = fieldsMap.keySet()
                .stream()
                .map(key -> id + delimiter + key)
                .toList();
        Integer expectedStylePrimary = 1;

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, eventMock);

        // Assert
        List<String> actualCustomIds = actual.stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().get())
                .toList();

        actual.stream().flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .forEach(messageComponent -> System.out.println(messageComponent.getData().style()));

        actual.stream().flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .forEach(messageComponent -> assertThat(messageComponent.getData().style().get()).isEqualTo(expectedStylePrimary));

        assertThat(actualCustomIds).containsExactlyInAnyOrderElementsOf(expectedCustomIds);
    }

    @Test
    void eventButtons_ReturnsListOfLayoutComponents_WhenFieldMapContainsOnlyDefaultFieldKeys() throws JsonProcessingException {
        // Arrange
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        String mapStructure = "mapStructure";
        when(embedTypeMock.getStructure()).thenReturn(mapStructure);
        String id = "123456";
        when(eventMock.getEventId()).thenReturn(id);
        String delimiter = ",";
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Tank",
                -2, "Melee",
                -3, "Ranged",
                -4, "Healer",
                -5, "Support"
        ));
        when(serializationMock.deserializeMap(mapStructure)).thenReturn(fieldsMap);
        List<String> expectedCustomIds = fieldsMap.keySet()
                .stream()
                .map(key -> id + delimiter + key)
                .toList();
        Integer expectedStyleSecondary = 2;

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, eventMock);

        // Assert
        List<String> actualCustomIds = actual.stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().get())
                .toList();

        actual.stream().flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .forEach(messageComponent -> assertThat(messageComponent.getData().style().get()).isEqualTo(expectedStyleSecondary));
        assertThat(actualCustomIds).containsExactlyInAnyOrderElementsOf(expectedCustomIds);
    }
    @Test
    void eventButtons_ReturnsEmptyListOfLayoutComponents_WhenSerializationExceptionThrown() throws JsonProcessingException {
        // Arrange
        Event eventMock = mock(Event.class);
        EmbedType embedTypeMock = mock(EmbedType.class);
        when(eventMock.getEmbedType()).thenReturn(embedTypeMock);
        String mapStructure = "mapStructure";
        when(embedTypeMock.getStructure()).thenReturn(mapStructure);
        String delimiter = ",";
        when(serializationMock.deserializeMap(mapStructure)).thenThrow(JsonProcessingException.class);

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, eventMock);

        // Assert
        System.out.println(actual);
        assertThat(actual).hasSize(0);
    }

}