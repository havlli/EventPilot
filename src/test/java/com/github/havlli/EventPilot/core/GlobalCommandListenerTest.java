package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.EventTypeComparator;
import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.command.onreadyevent.OnReadyEvent;
import com.github.havlli.EventPilot.command.onreadyevent.ScheduledTask;
import com.github.havlli.EventPilot.command.onreadyevent.StartupTask;
import com.github.havlli.EventPilot.command.test.TestCommand;
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
        underTest = new GlobalCommandListener(slashCommands, client, new EventTypeComparator());
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void constructListeners_WithMultipleCommands() {
        // Arrange
        slashCommands.add(new TestCommand());
        slashCommands.add(new TestCommand());

        // Act
        Flux<?> result = underTest.constructListeners();

        // Assert
        assertThat(result).isNotNull();
        verify(client, times(2)).on(any(), any());
    }

    @Test
    void constructListeners_WithOnReadyEventAsFirstToSubscribe() {
        // Arrange
        SlashCommand commandOne = new TestCommand();
        SlashCommand commandOnReadyEvent = new OnReadyEvent(mock(StartupTask.class), mock(ScheduledTask.class));
        SlashCommand commandTwo = new TestCommand();

        slashCommands.add(commandOne);
        slashCommands.add(commandOnReadyEvent);
        slashCommands.add(commandTwo);

        // Act
        Flux<?> result = underTest.constructListeners();

        // Assert
        assertThat(result).isNotEqualTo(Flux.empty());
        assertThat(slashCommands.get(0)).isEqualTo(commandOnReadyEvent);
        verify(client, times(3)).on(any(), any());
    }

    @Test
    void constructListeners_WithOneCommand_ShouldInvoke() {
        // Arrange
        SlashCommand matchingCommand = mock(SlashCommand.class);
        when(matchingCommand.getName()).thenReturn("commandName");
        when(matchingCommand.handle(any())).thenReturn(Mono.empty());
        matchingCommand.setEventType(ChatInputInteractionEvent.class);

        slashCommands.add(matchingCommand);

        when(client.on(any(),any())).thenReturn(Flux.just(5));

        // Act
        underTest.constructListeners()
                .then()
                .block();

        // Assert
        verify(client, times(1)).on(any(), any());
    }

    @Test
    void constructListeners_WithNoMatchingCommand_ShouldNotInvoke() {
        // Arrange

        // Act
        underTest.constructListeners()
                .then()
                .block();

        // Assert
        verify(client, never()).on(any(),any());
    }

    @Test
    void constructListeners_WithCommandHandlingError_ShouldHandleErrorGracefully() {
        // Arrange
        SlashCommand errorCommand = mock(SlashCommand.class);
        errorCommand.setEventType(ChatInputInteractionEvent.class);
        when(errorCommand.handle(any())).thenReturn(Mono.error(new RuntimeException("Command handling error")));

        slashCommands.add(errorCommand);

        AtomicBoolean errorHandled = new AtomicBoolean(false);

        // Act
        underTest.constructListeners()
                .onErrorResume(error -> {
                    errorHandled.set(true);
                    return Mono.empty();
                })
                .then()
                .block();

        // Assert
        assertThat(errorHandled.get()).isTrue();
    }
}