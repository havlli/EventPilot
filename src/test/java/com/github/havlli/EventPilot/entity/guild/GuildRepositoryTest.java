package com.github.havlli.EventPilot.entity.guild;

import com.github.havlli.EventPilot.TestDatabaseContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GuildRepositoryTest extends TestDatabaseContainer {

    private static final Logger LOG = LoggerFactory.getLogger(GuildRepositoryTest.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GuildRepository underTest;

    @BeforeEach
    void beforeEach() {
        LOG.info("Number of beans initialized { {} }", applicationContext.getBeanDefinitionCount());
    }

    @Test
    void saveGuild_SavesGuild_WhenGuildNotExists() {
        // Arrange
        Guild expected = new Guild("123","guild");

        // Act
        underTest.save(expected);

        // Assert
        List<Guild> actualGuilds = underTest.findAll();
        List<Guild> expectedGuilds = List.of(expected);
        assertThat(actualGuilds).usingRecursiveComparison()
                .isEqualTo(expectedGuilds);
    }

    @Test
    void saveGuild_UpdatesGuild_WhenGuildAlreadyExists() {
        // Arrange
        Guild expected = new Guild("123","guild1");
        underTest.save(expected);

        Guild updated = new Guild("123","guild2");

        // Act
        underTest.save(updated);

        // Assert
        List<Guild> actualGuilds = underTest.findAll();
        List<Guild> expectedGuilds = List.of(updated);
        assertThat(actualGuilds).usingRecursiveComparison()
                .isEqualTo(expectedGuilds);
    }

    @Test
    void getAllGuilds_ReturnsListOfGuilds_WhenGuildsExists() {
        // Arrange
        Guild expectedGuild1 = new Guild("123","guild1");
        Guild expectedGuild2 = new Guild("234","guild2");
        underTest.save(expectedGuild1);
        underTest.save(expectedGuild2);

        // Act
        List<Guild> actualGuilds = underTest.findAll();

        // Assert
        List<Guild> expectedGuilds = List.of(expectedGuild1, expectedGuild2);
        assertThat(actualGuilds).usingRecursiveComparison()
                .isEqualTo(expectedGuilds);
    }

    @Test
    void getAllGuilds_ReturnsEmptyList_WhenNoGuildExists() {
        // Act
        List<Guild> actualGuilds = underTest.findAll();

        // Assert
        assertThat(actualGuilds).isEmpty();
        assertThat(actualGuilds).hasSize(0);
    }

    @Test
    void selectGuildById_ReturnsGuildOptional_WhenGuildExists() {
        // Arrange
        Guild expectedGuild = new Guild("123","guild1");
        underTest.save(expectedGuild);

        // Act
        Optional<Guild> actualGuild = underTest.findById(expectedGuild.getId());

        // Assert
        Optional<Guild> expectedOptional = Optional.of(expectedGuild);
        assertThat(actualGuild).isPresent()
                .usingRecursiveComparison().isEqualTo(expectedOptional);
    }

    @Test
    void selectGuildById_ReturnsEmptyOptional_WhenNoGuildExists() {
        // Arrange
        Guild notExistingGuild = new Guild("123","guild1");

        // Act
        Optional<Guild> actualGuild = underTest.findById(notExistingGuild.getId());

        // Assert
        Optional<Guild> expectedOptional = Optional.empty();
        assertThat(actualGuild).isEmpty()
                .usingRecursiveComparison().isEqualTo(expectedOptional);
    }

    @Test
    void existsGuildById_ReturnsTrue_WhenGuildExists() {
        // Arrange
        Guild existingGuild = new Guild("123","guild1");
        underTest.save(existingGuild);

        // Act
        boolean actual = underTest.existsById(existingGuild.getId());

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void existsGuildById_ReturnsFalse_WhenGuildExists() {
        // Arrange
        Guild existingGuild = new Guild("123","guild1");

        // Act
        boolean actual = underTest.existsById(existingGuild.getId());

        // Assert
        assertThat(actual).isFalse();
    }
}