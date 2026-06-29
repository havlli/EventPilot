package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.entity.embedtype.EmbedType;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventSignupResult;
import com.github.havlli.EventPilot.entity.event.EventSignupService;
import com.github.havlli.EventPilot.entity.guild.Guild;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionCallbackSpec;
import discord4j.core.spec.InteractionCallbackSpecDeferEditMono;
import discord4j.core.spec.InteractionReplyEditSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventSignupCommandTest {

    private AutoCloseable autoCloseable;
    private EventSignupCommand underTest;
    @Mock
    private EventSignupService eventSignupService;
    @Mock
    private EmbedGenerator embedGenerator;
    @Mock
    private MessageSource messageSource;
    @Mock
    private ButtonInteractionEvent buttonEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private User user;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new EventSignupCommand(eventSignupService, embedGenerator, messageSource);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_ReturnsEmptyMono_WhenButtonIdIsNotSignupId() {
        // Arrange
        when(buttonEvent.getCustomId()).thenReturn("confirm");

        // Act
        Mono<Message> actual = underTest.handle(buttonEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(eventSignupService);
    }

    @Test
    void handle_ReturnsEmptyMono_WhenCommaButtonIdIsNotSignupId() {
        // Arrange
        when(buttonEvent.getCustomId()).thenReturn("confirm,1");

        // Act
        Mono<Message> actual = underTest.handle(buttonEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(eventSignupService);
    }

    @Test
    void handle_AppliesSignupAndEditsReply_WhenSignupIsSuccessful() {
        // Arrange
        Event event = createEvent();
        Message message = mock(Message.class);
        when(buttonEvent.getCustomId()).thenReturn("12345,1");
        when(buttonEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(Snowflake.of("999"));
        when(user.getUsername()).thenReturn("player");
        when(eventSignupService.applySignup("12345", "999", "player", 1))
                .thenReturn(EventSignupResult.added(event));
        when(buttonEvent.deferEdit()).thenReturn(InteractionCallbackSpecDeferEditMono.of(buttonEvent));
        when(buttonEvent.deferEdit(any(InteractionCallbackSpec.class))).thenReturn(Mono.empty());
        when(buttonEvent.editReply(any(InteractionReplyEditSpec.class))).thenReturn(Mono.just(message));
        when(embedGenerator.generateEmbed(event)).thenReturn(EmbedCreateSpec.builder().build());
        when(embedGenerator.generateComponents(event)).thenReturn(List.<TopLevelMessageComponent>of());

        // Act
        Mono<Message> actual = underTest.handle(buttonEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(message)
                .verifyComplete();
        verify(eventSignupService, times(1)).applySignup("12345", "999", "player", 1);
        verify(embedGenerator, times(1)).generateEmbed(event);
        verify(embedGenerator, times(1)).generateComponents(event);
    }

    @Test
    void handle_RepliesEphemerally_WhenEventIsFull() {
        // Arrange
        when(buttonEvent.getCustomId()).thenReturn("12345,1");
        when(buttonEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(Snowflake.of("999"));
        when(user.getUsername()).thenReturn("player");
        when(eventSignupService.applySignup("12345", "999", "player", 1))
                .thenReturn(EventSignupResult.withoutEvent(EventSignupResult.Outcome.EVENT_FULL));
        when(messageSource.getMessage("interaction.signup.event-full", null, Locale.ENGLISH))
                .thenReturn("This event is already full.");
        when(buttonEvent.reply()).thenReturn(InteractionApplicationCommandCallbackReplyMono.of(buttonEvent));
        when(buttonEvent.reply(any(InteractionApplicationCommandCallbackSpec.class))).thenReturn(Mono.empty());

        // Act
        Mono<?> actual = underTest.handle(buttonEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(messageSource, times(1)).getMessage("interaction.signup.event-full", null, Locale.ENGLISH);
        verify(buttonEvent, times(1)).reply(any(InteractionApplicationCommandCallbackSpec.class));
    }

    @Test
    void handle_AppliesSignupAndEditsReply_WhenSignupIsWaitlisted() {
        // Arrange
        Event event = createEvent();
        Message message = mock(Message.class);
        when(buttonEvent.getCustomId()).thenReturn("12345,1");
        when(buttonEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(Snowflake.of("999"));
        when(user.getUsername()).thenReturn("player");
        when(eventSignupService.applySignup("12345", "999", "player", 1))
                .thenReturn(EventSignupResult.waitlisted(event));
        when(buttonEvent.deferEdit()).thenReturn(InteractionCallbackSpecDeferEditMono.of(buttonEvent));
        when(buttonEvent.deferEdit(any(InteractionCallbackSpec.class))).thenReturn(Mono.empty());
        when(buttonEvent.editReply(any(InteractionReplyEditSpec.class))).thenReturn(Mono.just(message));
        when(embedGenerator.generateEmbed(event)).thenReturn(EmbedCreateSpec.builder().build());
        when(embedGenerator.generateComponents(event)).thenReturn(List.<TopLevelMessageComponent>of());

        // Act
        Mono<Message> actual = underTest.handle(buttonEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(message)
                .verifyComplete();
        verify(eventSignupService, times(1)).applySignup("12345", "999", "player", 1);
        verify(embedGenerator, times(1)).generateEmbed(event);
        verify(embedGenerator, times(1)).generateComponents(event);
    }

    @Test
    void handle_RepliesEphemerally_WhenEventIsClosed() {
        // Arrange
        when(buttonEvent.getCustomId()).thenReturn("12345,1");
        when(buttonEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(Snowflake.of("999"));
        when(user.getUsername()).thenReturn("player");
        when(eventSignupService.applySignup("12345", "999", "player", 1))
                .thenReturn(EventSignupResult.withoutEvent(EventSignupResult.Outcome.EVENT_CLOSED));
        when(messageSource.getMessage("interaction.signup.event-closed", null, Locale.ENGLISH))
                .thenReturn("This event is closed.");
        when(buttonEvent.reply()).thenReturn(InteractionApplicationCommandCallbackReplyMono.of(buttonEvent));
        when(buttonEvent.reply(any(InteractionApplicationCommandCallbackSpec.class))).thenReturn(Mono.empty());

        // Act
        Mono<?> actual = underTest.handle(buttonEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(messageSource, times(1)).getMessage("interaction.signup.event-closed", null, Locale.ENGLISH);
        verify(buttonEvent, times(1)).reply(any(InteractionApplicationCommandCallbackSpec.class));
    }

    @Test
    void getName_ReturnsEventSignup() {
        assertThat(underTest.getName()).isEqualTo("event-signup");
    }

    private Event createEvent() {
        return new Event(
                "12345",
                "name",
                "description",
                "author",
                Instant.now().plusSeconds(3600),
                "123",
                null,
                "25",
                new ArrayList<>(),
                new Guild("1", "guild"),
                new EmbedType(1L, "default", "{}", null)
        );
    }
}
