package com.github.havlli.EventPilot.core;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SimplePermissionValidatorTest {

    private AutoCloseable autoCloseable;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Member member;
    @Mock
    private Interaction interaction;
    @Mock
    private MessageCreator messageCreator;
    @Mock
    private Message messageMock;
    private SimplePermissionValidator underTest;

    @BeforeEach
    public void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new SimplePermissionValidator(messageCreator);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void followup_ReturnsNotValidMemberMessage_WhenMemberIsEmpty() {
        // Arrange
        Interaction interaction = mock(Interaction.class);
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.empty());

        Mono<Message> expected = Mono.just(messageMock);
        when(messageCreator.notValidMember(interactionEvent)).thenReturn(expected);

        // Act
        Mono<Message> actual = underTest.followupWith(
                Mono.empty(), interactionEvent,
                Permission.MANAGE_CHANNELS
        );

        // Assert
        StepVerifier.create(actual)
                .expectNext(messageMock)
                .verifyComplete();
        verify(messageCreator, times(1))
                .notValidMember(interactionEvent);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void followup_ReturnsFollowup_WhenMemberHasPermission() {
        // Arrange
        Permission currentPermission = Permission.MANAGE_CHANNELS;
        PermissionSet permissionSet = PermissionSet.of(currentPermission);
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.of(member));
        when(member.getBasePermissions()).thenReturn(Mono.just(permissionSet));

        Message expectedMessage = mock(Message.class);
        Mono<Message> expectedMono = Mono.just(expectedMessage);

        // Act
        Mono<Message> actualMono = underTest.followupWith(
                expectedMono, interactionEvent,
                currentPermission
        );

        // Assert
        StepVerifier.create(actualMono)
                .expectNext(expectedMessage)
                .verifyComplete();
        assertThat(actualMono.block())
                .isEqualTo(expectedMono.block());
    }

    @Test
    public void followup_ReturnsPermissionsNotValid_WhenMemberHasNoValidPermission() {
        // Arrange
        Permission currentPermission = Permission.CONNECT;
        PermissionSet permissionSet = PermissionSet.of(currentPermission);
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getMember()).thenReturn(Optional.of(member));
        when(member.getBasePermissions()).thenReturn(Mono.just(permissionSet));

        Message expectedMessage = mock(Message.class);
        Mono<Message> expectedMono = Mono.just(expectedMessage);
        when(messageCreator.permissionsNotValid(interactionEvent)).thenReturn(expectedMono);

        // Act
        Mono<Message> actualMono = underTest.followupWith(
                Mono.empty(), interactionEvent,
                Permission.MANAGE_CHANNELS
        );

        // Assert
        StepVerifier.create(actualMono)
                .expectNext(expectedMessage)
                .verifyComplete();
        assertThat(actualMono.block())
                .isEqualTo(expectedMono.block());
    }
}