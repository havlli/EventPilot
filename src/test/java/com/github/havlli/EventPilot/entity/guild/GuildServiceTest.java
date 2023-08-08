package com.github.havlli.EventPilot.entity.guild;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GuildServiceTest {

    private AutoCloseable autoCloseable;
    @Mock
    private GuildDAO guildDAO;
    @InjectMocks
    private GuildService underTest;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void saveGuild() {
        // Arrange
        Guild guildMock = mock(Guild.class);

        // Act
        underTest.saveGuild(guildMock);

        // Assert
        verify(guildDAO, times(1)).insertGuild(guildMock);
    }

    @Test
    public void getAllGuilds() {
        underTest.getAllGuilds();

        // Assert
        verify(guildDAO, times(1)).selectAllGuilds();
    }

    @Test
    public void getGuildById_WillReturnGuild_WhenExists() {
        // Arrange
        Guild guildMock = mock(Guild.class);
        String id = "1";
        when(guildDAO.selectGuildById(id)).thenReturn(Optional.of(guildMock));

        // Act
        Guild result = underTest.getGuildById(id);

        // Assert
        assertThat(result).isEqualTo(guildMock);
        verify(guildDAO, times(1)).selectGuildById(id);
    }

    @Test
    public void getGuildById_WillThrow_WhenNotExists() {
        // Arrange
        String id = "1";
        when(guildDAO.selectGuildById(id)).thenReturn(Optional.empty());

        // Assert thrown
        assertThatThrownBy(() -> underTest.getGuildById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Guild with id {%s} was not found!".formatted(id));
        verify(guildDAO, times(1)).selectGuildById(id);
    }

    @Test
    public void createGuildIfNotExists_WillCreateGuild_WhenNoExists() {
        // Arrange
        String id = "1";
        String name = "guildName";
        when(guildDAO.existsGuildById(id)).thenReturn(false);

        // Act
        underTest.createGuildIfNotExists(id, name);

        // Assert
        verify(guildDAO, times(1)).insertGuild(any());
    }

    @Test
    public void createGuildIfNotExists_WillNotCreateGuild_WhenAlreadyExists() {
        // Arrange
        String id = "1";
        String name = "guildName";
        when(guildDAO.existsGuildById(id)).thenReturn(true);

        // Act
        underTest.createGuildIfNotExists(id, name);

        // Assert
        verify(guildDAO, never()).insertGuild(any());
    }

    @Test
    public void existsGuildById() {
        // Arrange
        String id = "1";

        // Act
        underTest.existsGuildById(id);

        // Assert
        verify(guildDAO, times(1)).existsGuildById(id);
    }
}