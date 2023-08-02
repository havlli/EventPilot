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
        underTest.selectGuildById(id);

        // Assert
        verify(guildRepository, times(1)).findById(id);
    }

    @Test
    void selectAllGuilds() {
        // Act
        underTest.selectAllGuilds();

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
        underTest.insertGuild(guild);

        // Assert
        verify(guildRepository, times(1)).save(guild);
    }
}