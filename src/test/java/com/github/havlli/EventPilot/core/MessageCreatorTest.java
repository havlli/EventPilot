package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionFollowupCreateMono;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MessageCreatorTest {

    private AutoCloseable autoCloseable;

    private MessageCreator underTest;
    @Mock
    private MessageSource messageSourceMock;
    @Mock
    private ChatInputInteractionEvent interactionEvent;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new MessageCreator(messageSourceMock);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void createFollowupEphemeral() {
        // Arrange
        InteractionFollowupCreateMono followupCreateMono = mock(InteractionFollowupCreateMono.class);
        when(interactionEvent.createFollowup(anyString())).thenReturn(followupCreateMono);
        when(messageSourceMock.getMessage("permissions.not-valid", null, Locale.ENGLISH))
                .thenReturn("You do not have permission to use this command.");

        // Act
        underTest.permissionsNotValid(interactionEvent);

        // Assert
        verify(interactionEvent, times(1))
                .createFollowup("You do not have permission to use this command.");
        verify(followupCreateMono, times(1)).withEphemeral(true);
    }

    @Test
    public void notValidMember() {
        // Arrange
        InteractionFollowupCreateMono followupCreateMono = mock(InteractionFollowupCreateMono.class);
        when(interactionEvent.createFollowup(anyString())).thenReturn(followupCreateMono);
        when(messageSourceMock.getMessage("permissions.not-valid-member", null, Locale.ENGLISH))
                .thenReturn("You are not valid member of this guild!");

        // Act
        underTest.notValidMember(interactionEvent);

        // Assert
        verify(interactionEvent, times(1))
                .createFollowup("You are not valid member of this guild!");
        verify(followupCreateMono, times(1)).withEphemeral(true);
    }

    @Test
    public void sessionAlreadyActive() {
        // Arrange
        InteractionFollowupCreateMono followupCreateMono = mock(InteractionFollowupCreateMono.class);
        when(interactionEvent.createFollowup(anyString())).thenReturn(followupCreateMono);
        when(messageSourceMock.getMessage("sessions.already-active-session", null, Locale.ENGLISH))
                .thenReturn("You have already one active interaction, finish previous interaction to continue!");

        // Act
        underTest.sessionAlreadyActive(interactionEvent);

        // Assert
        verify(interactionEvent, times(1))
                .createFollowup("You have already one active interaction, finish previous interaction to continue!");
        verify(followupCreateMono, times(1)).withEphemeral(true);
    }
}