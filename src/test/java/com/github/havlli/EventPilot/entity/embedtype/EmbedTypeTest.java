package com.github.havlli.EventPilot.entity.embedtype;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbedTypeTest {

    private EmbedType underTest;

    @BeforeEach
    void setUp() {
        underTest = new EmbedType(1L,"test","placeholder", List.of());
    }

    @Test
    void testToString() {
        // Arrange
        String expected = "EmbedType{id=1, name='test', structure='placeholder'}";

        // Act
        String actual = underTest.toString();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void builder_WillBuildEmbedType() {
        // Arrange
        String name = "test";
        String structure = "structure";
        EmbedType expected = new EmbedType(name, structure, List.of());

        // Act
        EmbedType actual = EmbedType.builder()
                .withName(name)
                .withStructure(structure)
                .build();

        // Assert
        assertThat(actual).usingRecursiveAssertion().isEqualTo(expected);
    }

    @Test
    void builder_WillThrowException_WhenNameIsNull() {
        // Arrange
        String structure = "structure";

        // Assert
        assertThatThrownBy(() -> EmbedType.builder()
                .withStructure(structure)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void builder_WillThrowException_WhenStructureIsNull() {
        // Arrange
        String name = "test";
        // Assert
        assertThatThrownBy(() -> EmbedType.builder()
                .withName(name)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("structure is required");
    }

    @Test
    void builderGetEmbedType_ReturnsObject_WhenObjectIsBuilt() {
        // Arrange
        String name = "test";
        String structure = "structure";
        EmbedType expected = new EmbedType(name, structure, List.of());

        EmbedType.Builder builder = EmbedType.builder()
                .withName(name)
                .withStructure(structure);
        builder.build();
        // Act
        EmbedType actual = builder.getEmbedType();

        // Assert
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void builderGetEmbedType_WillThrowException_WhenObjectNotBuiltYet() {
        // Assert
        assertThatThrownBy(() -> EmbedType.builder().getEmbedType())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot retrieve event that was not built yet!");
    }
}