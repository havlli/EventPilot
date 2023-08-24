package com.github.havlli.EventPilot.component;

import com.github.havlli.EventPilot.component.ButtonRow.Builder.ButtonType;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ButtonRowTest {

    @Test
    void builder_ReturnsCompleteObject_WhenBuildIsCalledAndAtLeastOneButtonDefined() {
        // Arrange
        String customId = "test_id";
        String label = "test label";
        ButtonType buttonType = ButtonType.PRIMARY;

        // Act
        ButtonRow actual = ButtonRow.builder()
                .addButton(customId, label, buttonType)
                .build();

        // Assert
        assertThat(actual.getCustomIds()).containsOnly(customId);
    }

    @Test
    void builder_ThrowsInvalidStateException_WhenBuildIsCalledAndNoButtonWasDefined() {
        // Arrange
        ButtonRow.Builder builder = ButtonRow.builder();

        // Assert
        assertThatThrownBy(() -> builder.build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("To construct ButtonRow you need to define at least one button!");
    }

    @Test
    void getCustomIds_ReturnsListOfCustomIds_WhenMultipleButtonsDefined() {
        // Arrange
        String buttonOneCustomId = "button_one";
        String buttonTwoCustomId = "button_two";
        String buttonThreeCustomId = "button_three";

        ButtonRow buttonRow = ButtonRow.builder()
                .addButton(buttonOneCustomId, "label", ButtonType.PRIMARY)
                .addButton(buttonTwoCustomId, "label", ButtonType.SECONDARY)
                .addButton(buttonThreeCustomId, "label", ButtonType.DANGER)
                .build();

        // Act
        List<String> customIds = buttonRow.getCustomIds();

        // Assert
        assertThat(customIds).containsOnly(buttonOneCustomId, buttonTwoCustomId, buttonThreeCustomId);
    }

    @Test
    void getCustomIds_ReturnsListOfCustomIds_WhenOnlyOneButtonDefined() {
        // Arrange
        String buttonOneCustomId = "button_one";

        ButtonRow buttonRow = ButtonRow.builder()
                .addButton(buttonOneCustomId, "label", ButtonType.PRIMARY)
                .build();

        // Act
        List<String> customIds = buttonRow.getCustomIds();

        // Assert
        assertThat(customIds).containsOnly(buttonOneCustomId);
    }

    @Test
    void getActionRow_ReturnsActionRow_WhenMultipleButtonsDefined() {
        // Arrange
        String buttonOneCustomId = "button_one";
        String buttonTwoCustomId = "button_two";
        String buttonThreeCustomId = "button_three";

        ButtonRow buttonRow = ButtonRow.builder()
                .addButton(buttonOneCustomId, "label", ButtonType.PRIMARY)
                .addButton(buttonTwoCustomId, "label", ButtonType.SECONDARY)
                .addButton(buttonThreeCustomId, "label", ButtonType.DANGER)
                .build();

        // Act
        ActionRow actual = buttonRow.getActionRow();

        // Assert
        List<Optional<String>> actualList = actual.getChildren().stream()
                .filter(c -> c instanceof Button)
                .map(c -> ((Button) c).getCustomId())
                .toList();

        assertThat(actualList).containsOnly(
                Optional.of(buttonOneCustomId),
                Optional.of(buttonTwoCustomId),
                Optional.of(buttonThreeCustomId)
        );
    }

    @Test
    void getActionRow_ReturnsActionRow_WhenOnlyOneButtonsDefined() {
        // Arrange
        String buttonOneCustomId = "button_one";

        ButtonRow buttonRow = ButtonRow.builder()
                .addButton(buttonOneCustomId, "label", ButtonType.PRIMARY)
                .build();

        // Act
        ActionRow actual = buttonRow.getActionRow();

        // Assert
        List<Optional<String>> actualList = actual.getChildren().stream()
                .filter(c -> c instanceof Button)
                .map(c -> ((Button) c).getCustomId())
                .toList();

        assertThat(actualList).containsOnly(Optional.of(buttonOneCustomId));
    }

    @Test
    void getDisabledRow_ReturnsDisabledRow_WhenMultipleButtonsDefined() {
        // Arrange
        String buttonOneCustomId = "button_one";
        String buttonTwoCustomId = "button_two";
        String buttonThreeCustomId = "button_three";

        ButtonRow buttonRow = ButtonRow.builder()
                .addButton(buttonOneCustomId, "label", ButtonType.PRIMARY)
                .addButton(buttonTwoCustomId, "label", ButtonType.SECONDARY)
                .addButton(buttonThreeCustomId, "label", ButtonType.DANGER)
                .build();

        // Act
        ActionRow actual = buttonRow.getDisabledRow();

        // Assert
        List<Button> actualList = actual.getChildren().stream()
                .filter(c -> c instanceof Button)
                .map(c -> ((Button) c))
                .toList();

        assertThat(actualList).hasSize(3);
        actualList.forEach(button -> assertThat(button.isDisabled()).isTrue());
    }

    @Test
    void getDisabledRow_ReturnsDisabledRow_WhenOnlyOneButtonDefined() {
        // Arrange
        String buttonOneCustomId = "button_one";

        ButtonRow buttonRow = ButtonRow.builder()
                .addButton(buttonOneCustomId, "label", ButtonType.PRIMARY)
                .build();

        // Act
        ActionRow actual = buttonRow.getDisabledRow();

        // Assert
        List<Button> actualList = actual.getChildren().stream()
                .filter(c -> c instanceof Button)
                .map(c -> ((Button) c))
                .toList();

        assertThat(actualList).hasSize(1);
        actualList.forEach(button -> assertThat(button.isDisabled()).isTrue());
    }
}