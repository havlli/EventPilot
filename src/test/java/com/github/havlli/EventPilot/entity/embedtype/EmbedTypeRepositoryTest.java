package com.github.havlli.EventPilot.entity.embedtype;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EmbedTypeRepositoryTest extends TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(EmbedTypeRepositoryTest.class);
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private EmbedTypeRepository underTest;

    @BeforeEach
    void setUp() {
        LOG.info("Number of beans initialized { {} }", applicationContext.getBeanDefinitionCount());
    }

    @Test
    void findAll() {
        // Arrange
        EmbedType embedType = new EmbedType(
                1,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );
        underTest.save(embedType);

        // Act
        List<EmbedType> actual = underTest.findAll();

        // Assert
        assertThat(actual).containsOnly(embedType);
    }

    @Test
    void findById_ReturnsOptionalEmbedType_WhenPresent() {
        // Arrange
        EmbedType embedType = new EmbedType(
                1,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );
        underTest.save(embedType);

        // Act
        Optional<EmbedType> actual = underTest.findById(embedType.getId());

        // Assert
        assertThat(actual).hasValue(embedType);
    }

    @Test
    void findById_ReturnsEmptyOptional_WhenNotPresent() {
        // Act
        Optional<EmbedType> actual = underTest.findById(1);

        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    void save() {
        // Arrange
        EmbedType embedType = new EmbedType(
                1,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );

        // Act
        underTest.save(embedType);

        // Assert
        List<EmbedType> actual = underTest.findAll();
        assertThat(actual).containsOnly(embedType);
    }

    @Test
    void existsEmbedTypeById_ReturnsTrue_WhenEmbedTypeExists() {
        // Arrange
        EmbedType embedType = new EmbedType(
                1,
                "test",
                "{\"-1\":\"Absence\",\"-2\":\"Late\",\"1\":\"Tank\",\"-3\":\"Tentative\",\"2\":\"Melee\",\"3\":\"Ranged\",\"4\":\"Healer\",\"5\":\"Support\"}",
                new ArrayList<>()
        );
        underTest.save(embedType);

        // Act
        boolean actual = underTest.existsById(embedType.getId());

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void existsEmbedTypeById_ReturnsFalse_WhenEmbedTypeNotExists() {
        // Act
        boolean actual = underTest.existsById(1);

        // Assert
        assertThat(actual).isFalse();
    }
}