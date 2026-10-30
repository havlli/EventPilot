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
    public void getGuildById_WillReturnGuild_WhenExists() {
        // Arrange
        Guild guildMock = mock(Guild.class);
        String id = "1";
        when(guildDAO.getGuildById(id)).thenReturn(Optional.of(guildMock));

        // Act
        Guild result = underTest.getGuildById(id);

        // Assert
        assertThat(result).isEqualTo(guildMock);
        verify(guildDAO, times(1)).getGuildById(id);
    }

    @Test
    public void getGuildById_WillThrow_WhenNotExists() {
        // Arrange
        String id = "1";
        when(guildDAO.getGuildById(id)).thenReturn(Optional.empty());

        // Assert thrown
        assertThatThrownBy(() -> underTest.getGuildById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Guild with id {%s} was not found!".formatted(id));
        verify(guildDAO, times(1)).getGuildById(id);
    }

    @Test
    public void createGuildIfNotExists_WillCreateGuild_WhenNoExists() {
        // Arrange
        String id = "1";
        String name = "guildName";

        // Act
        underTest.createGuildIfNotExists(id, name);

        // Assert
        verify(guildDAO, times(1)).saveGuildIfNotExists(id, name);
    }

    @Test
    public void createGuildIfNotExists_WillNotCreateGuild_WhenAlreadyExists() {
        // Arrange
        String id = "1";
        String name = "guildName";

        // Act
        underTest.createGuildIfNotExists(id, name);

        // Assert
        verify(guildDAO).saveGuildIfNotExists(id, name);
        verifyNoMoreInteractions(guildDAO);
    }

}