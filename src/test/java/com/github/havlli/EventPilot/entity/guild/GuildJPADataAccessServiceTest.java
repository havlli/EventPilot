package com.github.havlli.EventPilot.entity.guild;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class GuildJPADataAccessServiceTest {

    private AutoCloseable autoCloseable;
    @Mock
    private GuildRepository guildRepository;
    @InjectMocks
    private GuildJPADataAccessService underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void selectGuildById() {
        // Arrange
        String id = "1";

        // Act
        underTest.getGuildById(id);

        // Assert
        verify(guildRepository, times(1)).findById(id);
    }

    @Test
    void selectAllGuilds() {
        // Act
        underTest.getGuilds();

        // Assert
        verify(guildRepository, times(1)).findAll();
    }

    @Test
    void existsGuildById() {
        // Arrange
        String id = "1";

        // Act
        underTest.existsGuildById(id);

        // Assert
        verify(guildRepository, times(1)).existsById(id);
    }

    @Test
    void insertGuild() {
        // Arrange
        Guild guild = mock(Guild.class);

        // Act
        underTest.saveGuild(guild);

        // Assert
        verify(guildRepository, times(1)).save(guild);
    }

    @Test
    void saveGuildIfNotExists() {
        // Arrange
        String id = "1";
        String name = "test";

        // Act
        underTest.saveGuildIfNotExists(id, name);

        // Assert
        verify(guildRepository, times(1)).saveGuildIfNotExists(id, name);
    }
}