package com.github.havlli.EventPilot.prompt;

import com.github.havlli.EventPilot.component.ButtonRowComponent;
import com.github.havlli.EventPilot.component.SelectMenuComponent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptFilterTest {

    private AutoCloseable autoCloseable;

    @InjectMocks
    private PromptFilter underTest;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void selectInteractionEvent_ReturnsTrue_WhenIsSameUserAndComponent() {
        // Arrange
        User user = mock(User.class);
        SelectMenuComponent selectMenuComponent = mock(SelectMenuComponent.class);
        when(selectMenuComponent.getCustomId()).thenReturn("custom-id");
        SelectMenuInteractionEvent event = mock(SelectMenuInteractionEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(event.getCustomId()).thenReturn("custom-id");

        // Act
        Predicate<SelectMenuInteractionEvent> predicate = underTest.selectInteractionEvent(selectMenuComponent, user);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void selectInteractionEvent_ReturnsFalse_WhenIsDifferentUser() {
        // Arrange
        User userOne = mock(User.class);
        User userTwo = mock(User.class);
        SelectMenuComponent selectMenuComponent = mock(SelectMenuComponent.class);
        when(selectMenuComponent.getCustomId()).thenReturn("custom-id");
        SelectMenuInteractionEvent event = mock(SelectMenuInteractionEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(userOne);
        when(event.getCustomId()).thenReturn("custom-id");

        // Act
        Predicate<SelectMenuInteractionEvent> predicate = underTest.selectInteractionEvent(selectMenuComponent, userTwo);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void selectInteractionEvent_ReturnsFalse_WhenIsDifferentComponent() {
        // Arrange
        User user = mock(User.class);
        SelectMenuComponent selectMenuComponent = mock(SelectMenuComponent.class);
        when(selectMenuComponent.getCustomId()).thenReturn("custom-id");
        SelectMenuInteractionEvent event = mock(SelectMenuInteractionEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(event.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(event.getCustomId()).thenReturn("different-id");

        // Act
        Predicate<SelectMenuInteractionEvent> predicate = underTest.selectInteractionEvent(selectMenuComponent, user);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void buttonInteractionEvent_ReturnsTrue_WhenIsSameUserAndComponent() {
        // Arrange
        User user = mock(User.class);
        ButtonRowComponent buttonRowComponent = mock(ButtonRowComponent.class);
        List<String> customIds = List.of("custom-id-1", "custom-id-2");
        when(buttonRowComponent.getCustomIds()).thenReturn(customIds);
        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(interaction.getUser()).thenReturn(user);
        when(event.getInteraction()).thenReturn(interaction);
        when(event.getCustomId()).thenReturn("custom-id-1");

        // Act
        Predicate<ButtonInteractionEvent> predicate = underTest.buttonInteractionEvent(buttonRowComponent, user);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void buttonInteractionEvent_ReturnsTrue_WhenIsDifferentUser() {
        // Arrange
        User userOne = mock(User.class);
        User userTwo = mock(User.class);
        ButtonRowComponent buttonRowComponent = mock(ButtonRowComponent.class);
        List<String> customIds = List.of("custom-id-1", "custom-id-2");
        when(buttonRowComponent.getCustomIds()).thenReturn(customIds);
        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(interaction.getUser()).thenReturn(userOne);
        when(event.getInteraction()).thenReturn(interaction);
        when(event.getCustomId()).thenReturn("custom-id-1");

        // Act
        Predicate<ButtonInteractionEvent> predicate = underTest.buttonInteractionEvent(buttonRowComponent, userTwo);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void buttonInteractionEvent_ReturnsTrue_WhenIsDifferentComponent() {
        // Arrange
        User user = mock(User.class);
        ButtonRowComponent buttonRowComponent = mock(ButtonRowComponent.class);
        List<String> customIds = List.of("custom-id-1", "custom-id-2");
        when(buttonRowComponent.getCustomIds()).thenReturn(customIds);
        ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
        Interaction interaction = mock(Interaction.class);
        when(interaction.getUser()).thenReturn(user);
        when(event.getInteraction()).thenReturn(interaction);
        when(event.getCustomId()).thenReturn("different-id-1");

        // Act
        Predicate<ButtonInteractionEvent> predicate = underTest.buttonInteractionEvent(buttonRowComponent, user);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isMessageAuthor_ReturnTrue_WhenUserIsAuthor() {
        // Arrange
        User user = mock(User.class);
        MessageCreateEvent event = mock(MessageCreateEvent.class);
        Message message = mock(Message.class);
        when(event.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.of(user));

        // Act
        Predicate<MessageCreateEvent> predicate = underTest.isMessageAuthor(user);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isMessageAuthor_ReturnTrue_WhenUserIsNotAuthor() {
        // Arrange
        User userOne = mock(User.class);
        User userTwo = mock(User.class);
        MessageCreateEvent event = mock(MessageCreateEvent.class);
        Message message = mock(Message.class);
        when(event.getMessage()).thenReturn(message);
        when(message.getAuthor()).thenReturn(Optional.of(userOne));

        // Act
        Predicate<MessageCreateEvent> predicate = underTest.isMessageAuthor(userTwo);
        boolean result = predicate.test(event);

        // Assert
        assertThat(result).isFalse();
    }
}