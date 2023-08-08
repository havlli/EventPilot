package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionFollowupCreateMono;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SimplePermissionCheckerTest {

    private AutoCloseable autoCloseable;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Member member;
    @Mock
    private Interaction interaction;
    @Mock
    private Message followupMessage;
    private SimplePermissionChecker underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void followup_WillReturnNotValidMemberMessage_WhenMemberIsEmpty() {
        // Arrange
        Interaction interaction = mock(Interaction.class);
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.empty());
        InteractionFollowupCreateMono interactionFollowupCreateMono = InteractionFollowupCreateMono.of(interactionEvent);
        when(interactionEvent.createFollowup(anyString()))
                .thenReturn(interactionFollowupCreateMono);

        String message = "You are not valid Member to use this command";
        Mono<Message> messageMono = Mono.empty();

        Mono<Message> expected = interactionFollowupCreateMono.withEphemeral(true);

        // Act
        underTest = new SimplePermissionChecker(interactionEvent, Permission.MANAGE_CHANNELS);
        Mono<Message> result = underTest.followup(messageMono);

        // Assert
        verify(interactionEvent, times(1))
                .createFollowup(message);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void followup_WillReturnFollowup_WhenMemberHasPermission() {
        // Arrange
        Permission currentPermission = Permission.MANAGE_CHANNELS;
        PermissionSet permissionSet = PermissionSet.of(currentPermission);
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.of(member));
        when(member.getBasePermissions()).thenReturn(Mono.just(permissionSet));

        Message message = mock(Message.class);
        Mono<Message> expectedMono = Mono.just(message);

        // Act
        underTest = new SimplePermissionChecker(interactionEvent, currentPermission);
        Mono<Message> actualMono = underTest.followup(expectedMono);

        // Assert
        assertThat(actualMono.block())
                .isEqualTo(expectedMono.block());
    }

    @Test
    void followup_WillReturnNotValidPermissionsMessageMono_WhenMemberHasNoPermission() {
        // Arrange
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.of(member));
        PermissionSet permissionSet = PermissionSet.of();
        when(member.getBasePermissions()).thenReturn(Mono.just(permissionSet));

        String message = "You do not have permission to use this command.";

        InteractionFollowupCreateMono interactionFollowupCreateMono = InteractionFollowupCreateMono.of(interactionEvent);
        when(interactionEvent.createFollowup(message)).thenReturn(interactionFollowupCreateMono);

        // Act
        underTest = new SimplePermissionChecker(interactionEvent, Permission.MANAGE_CHANNELS);
        underTest.followup(Mono.empty());

        // Assert
        verify(interactionEvent, times(1)).createFollowup(message);
    }
}