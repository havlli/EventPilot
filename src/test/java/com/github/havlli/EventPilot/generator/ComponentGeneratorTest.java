package com.github.havlli.EventPilot.generator;

import discord4j.core.object.component.LayoutComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentGeneratorTest {

    private ComponentGenerator underTest;

    @BeforeEach
    void setUp() {
        underTest = new ComponentGenerator();
    }

    @Test
    void eventButtons_ReturnsListOfLayoutComponents_WhenFieldMapContainsDefaultAndRoleFieldKeys() {
        // Arrange
        String id = "123456";
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
        List<String> expectedCustomIds = fieldsMap.keySet()
                .stream()
                .map(key -> id + delimiter + key)
                .toList();

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, id, fieldsMap);

        // Assert
        List<String> actualCustomIds = actual.stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().get())
                .toList();

        assertThat(actualCustomIds).containsExactlyInAnyOrderElementsOf(expectedCustomIds);
    }

    @Test
    void eventButtons_ReturnsListOfLayoutComponents_WhenFieldMapContainsOnlyRoleFieldKeys() {
        // Arrange
        String id = "123456";
        String delimiter = ",";
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                1, "Tank",
                2, "Melee",
                3, "Ranged",
                4, "Healer",
                5, "Support"
        ));
        List<String> expectedCustomIds = fieldsMap.keySet()
                .stream()
                .map(key -> id + delimiter + key)
                .toList();
        Integer expectedStylePrimary = 1;

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, id, fieldsMap);

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
    void eventButtons_ReturnsListOfLayoutComponents_WhenFieldMapContainsOnlyDefaultFieldKeys() {
        // Arrange
        String id = "123456";
        String delimiter = ",";
        HashMap<Integer, String> fieldsMap = new HashMap<>(Map.of(
                -1, "Tank",
                -2, "Melee",
                -3, "Ranged",
                -4, "Healer",
                -5, "Support"
        ));
        List<String> expectedCustomIds = fieldsMap.keySet()
                .stream()
                .map(key -> id + delimiter + key)
                .toList();
        Integer expectedStyleSecondary = 2;

        // Act
        List<LayoutComponent> actual = underTest.eventButtons(delimiter, id, fieldsMap);

        // Assert
        List<String> actualCustomIds = actual.stream()
                .flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .map(messageComponent -> messageComponent.getData().customId().get())
                .toList();

        actual.stream().flatMap(layoutComponent -> layoutComponent.getChildren().stream())
                .forEach(messageComponent -> assertThat(messageComponent.getData().style().get()).isEqualTo(expectedStyleSecondary));
        assertThat(actualCustomIds).containsExactlyInAnyOrderElementsOf(expectedCustomIds);
    }
}