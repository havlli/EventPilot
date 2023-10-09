package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.command.createevent.CreateEventCommand;
import com.github.havlli.EventPilot.command.createevent.CreateEventInteraction;
import com.github.havlli.EventPilot.command.onreadyevent.OnReadyEvent;
import com.github.havlli.EventPilot.command.onreadyevent.ScheduledTask;
import com.github.havlli.EventPilot.command.onreadyevent.StartupTask;
import com.github.havlli.EventPilot.command.test.TestCommand;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventTypeComparatorTest {

    private AutoCloseable autoCloseable;
    private EventTypeComparator underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EventTypeComparator();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void compareOnReadyEvents() {
        // Arrange
        SlashCommand command1 = new OnReadyEvent(mock(StartupTask.class),mock(ScheduledTask.class));
        SlashCommand command2 = new OnReadyEvent(mock(StartupTask.class),mock(ScheduledTask.class));

        // Act
        int result = underTest.compare(command1, command2);

        // Assert
        assertThat(result)
                .as("Both commands should have the same event type (ReadyEvent)")
                .isEqualTo(0);
    }

    @Test
    void compareWhenOnReadyEventFirst() {
        // Arrange
        SlashCommand command1 = new OnReadyEvent(mock(StartupTask.class),mock(ScheduledTask.class));
        SlashCommand command2 = new TestCommand();

        // Act
        int result = underTest.compare(command1, command2);

        // Assert
        assertThat(result)
                .as("The first command should have the OnReadyEvent, so it should come first")
                .isEqualTo(-1);
    }

    @Test
    void compareWhenOnReadyEventSecond() {
        // Arrange
        SlashCommand command1 = new TestCommand();
        SlashCommand command2 = new OnReadyEvent(mock(StartupTask.class),mock(ScheduledTask.class));

        // Act
        int result = underTest.compare(command1, command2);

        // Assert
        assertThat(result)
                .as("The second command should have the OnReadyEvent, so it should come first")
                .isEqualTo(1);
    }

    @Test
    void compareWhenTwoDifferentEvents() {
        // Arrange
        SlashCommand command1 = new TestCommand();
        SlashCommand command2 = new CreateEventCommand(mock(CreateEventInteraction.class), mock(SimplePermissionValidator.class), mock(UserSessionValidator.class), mock(MessageSource.class));

        // Act
        int result = underTest.compare(command1, command2);

        // Assert
        assertThat(result)
                .as("Commands with different event types should have a non-zero comparison result")
                .isEqualTo(0);
    }
}