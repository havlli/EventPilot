package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GlobalCommandListenerTest {

    private AutoCloseable autoCloseable;
    @Mock
    private GatewayDiscordClient client;

    private GlobalCommandListener underTest;
    private List<SlashCommand> slashCommands;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        slashCommands = new ArrayList<>();
        when(client.on(any(Class.class), any(Function.class))).thenReturn(Flux.empty());
        underTest = new GlobalCommandListener(slashCommands, client);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_WithMatchingCommand_ShouldInvokeCommandHandle() {
        // Arrange
        SlashCommand matchingCommand = mock(SlashCommand.class);
        when(matchingCommand.getName()).thenReturn("commandName");
        when(matchingCommand.handle(any())).thenReturn(Mono.empty());

        slashCommands.add(matchingCommand);

        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(event.getCommandName()).thenReturn("commandName");

        // Act
        underTest.handle(event).block();

        // Assert
        verify(matchingCommand, times(1)).handle(event);
    }

    @Test
    void handle_WithNoMatchingCommand_ShouldNotInvokeCommandHandle() {
        // Arrange
        SlashCommand nonMatchingCommand = mock(SlashCommand.class);
        when(nonMatchingCommand.getName()).thenReturn("commandName");
        when(nonMatchingCommand.handle(any())).thenReturn(Mono.empty());

        slashCommands.add(nonMatchingCommand);

        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(event.getCommandName()).thenReturn("differentCommandName");

        // Act
        underTest.handle(event).block();

        // Assert
        verify(nonMatchingCommand, never()).handle(event);
    }

    @Test
    void handle_WithCommandHandlingError_ShouldHandleErrorGracefully() {
        // Arrange
        SlashCommand errorCommand = mock(SlashCommand.class);
        when(errorCommand.getName()).thenReturn("commandName");
        when(errorCommand.handle(any())).thenReturn(Mono.error(new RuntimeException("Command handling error")));

        slashCommands.add(errorCommand);

        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(event.getCommandName()).thenReturn("commandName");

        AtomicBoolean errorHandled = new AtomicBoolean(false);

        // Act
        underTest.handle(event)
                .onErrorResume(error -> {
                    errorHandled.set(true);
                    return Mono.empty();
                })
                .block();

        // Assert
        assertThat(errorHandled.get()).isTrue();
    }
}