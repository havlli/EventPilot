package com.github.havlli.EventPilot.core;

import com.github.havlli.EventPilot.command.EventTypeComparator;
import com.github.havlli.EventPilot.command.SlashCommand;
import com.github.havlli.EventPilot.command.onreadyevent.OnReadyEvent;
import com.github.havlli.EventPilot.command.onreadyevent.ScheduledTask;
import com.github.havlli.EventPilot.command.onreadyevent.StartupTask;
import discord4j.core.event.domain.Event;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.reactivestreams.Publisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
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
        underTest = new GlobalCommandListener(slashCommands, client, new EventTypeComparator());
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void constructListeners_WithMultipleCommands() {
        // Arrange
        slashCommands.add(new TestSlashCommand("test-one"));
        slashCommands.add(new TestSlashCommand("test-two"));
        when(client.on(any(), any())).thenReturn(Flux.just("test"));

        // Act
        Flux<?> actual = underTest.createListeners();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNextCount(2L)
                .verifyComplete();
        verify(client, times(2)).on(any(), any());
    }

    @Test
    void constructListeners_WithOnReadyEventAsFirstToSubscribe() {
        // Arrange
        SlashCommand commandOne = new TestSlashCommand("test-one");
        SlashCommand commandOnReadyEvent = new OnReadyEvent(mock(StartupTask.class), mock(ScheduledTask.class));
        SlashCommand commandTwo = new TestSlashCommand("test-two");

        slashCommands.add(commandOne);
        slashCommands.add(commandOnReadyEvent);
        slashCommands.add(commandTwo);

        when(client.on(any(), any())).thenReturn(Flux.just("test"));

        // Act
        Flux<?> actual = underTest.createListeners();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNextCount(3L)
                .verifyComplete();
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

        when(client.on(any(), any())).thenReturn(Flux.just("test"));

        // Act
        Flux<?> actual = underTest.createListeners();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .expectNextCount(1L)
                .verifyComplete();
        verify(client, times(1)).on(any(), any());
    }

    @Test
    void constructListeners_WithNoMatchingCommand_ShouldNotInvoke() {
        // Act
        Flux<?> actual = underTest.createListeners();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(client, never()).on(any(), any());
    }

    @Test
    void constructListeners_WithListenerError_ShouldHandleErrorGracefully() {
        // Arrange
        SlashCommand errorCommand = mock(SlashCommand.class);
        errorCommand.setEventType(ChatInputInteractionEvent.class);
        when(client.on(any(), any())).thenReturn(Flux.error(new RuntimeException("Command handling error")));

        slashCommands.add(errorCommand);

        // Act
        Flux<?> actual = underTest.createListeners();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
    }

    @Test
    void constructListeners_WithCommandHandlingError_ShouldHandleErrorGracefully() {
        // Arrange
        SlashCommand errorCommand = mock(SlashCommand.class);
        ChatInputInteractionEvent event = mock(ChatInputInteractionEvent.class);
        when(errorCommand.getName()).thenReturn("error-command");
        doReturn(ChatInputInteractionEvent.class).when(errorCommand).getEventType();
        when(errorCommand.handle(event)).thenReturn(Mono.error(new RuntimeException("Command handling error")));
        when(client.on(eq(ChatInputInteractionEvent.class), any())).thenAnswer(invocation -> {
            Function<ChatInputInteractionEvent, Publisher<?>> handler = invocation.getArgument(1);
            return Flux.just(event)
                    .flatMap(nextEvent -> Mono.from(handler.apply(nextEvent)));
        });

        slashCommands.add(errorCommand);

        // Act
        Flux<?> actual = underTest.createListeners();

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(errorCommand, times(1)).handle(event);
    }

    private static class TestSlashCommand implements SlashCommand {
        private final String name;
        private Class<? extends Event> eventType = ChatInputInteractionEvent.class;

        private TestSlashCommand(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<? extends Event> getEventType() {
            return eventType;
        }

        @Override
        public void setEventType(Class<? extends Event> eventType) {
            this.eventType = eventType;
        }

        @Override
        public Mono<?> handle(Event event) {
            return Mono.empty();
        }
    }
}
