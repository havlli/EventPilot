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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "confirm",
            "confirm,1",
            "12345",
            "12345,",
            "12345,role",
            "12345,1,2",
            ",1"
    })
    void handle_ReturnsEmptyMono_WhenButtonIdIsNotSignupId(String customId) {
        // Arrange
        when(buttonEvent.getCustomId()).thenReturn(customId);

        // Act
        Mono<Message> actual = underTest.handle(buttonEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verifyNoInteractions(eventSignupService);
    }

    @ParameterizedTest
    @EnumSource(value = EventSignupResult.Outcome.class, names = {"ADDED", "UPDATED", "WAITLISTED"})
    void handle_AppliesSignupAndEditsReply_WhenSignupUpdatesEventMessage(EventSignupResult.Outcome outcome) {
        // Arrange
        Event event = createEvent();
        Message message = mock(Message.class);
        stubSignupRequest();
        when(eventSignupService.applySignup("12345", "999", "player", 1))
                .thenReturn(signupResult(outcome, event));
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
        verifyNoInteractions(messageSource);
    }

    @ParameterizedTest
    @EnumSource(value = EventSignupResult.Outcome.class, names = {
            "EVENT_FULL",
            "EVENT_NOT_FOUND",
            "ROLE_NOT_FOUND",
            "INVALID_CAPACITY",
            "EVENT_CLOSED",
            "EVENT_CANCELLED",
            "EVENT_EXPIRED"
    })
    void handle_RepliesEphemerally_WhenSignupIsBlocked(EventSignupResult.Outcome outcome) {
        // Arrange
        String messageKey = messageKeyFor(outcome);
        String message = "message for " + outcome;
        stubSignupRequest();
        when(eventSignupService.applySignup("12345", "999", "player", 1))
                .thenReturn(EventSignupResult.withoutEvent(outcome));
        when(messageSource.getMessage(messageKey, null, Locale.ENGLISH)).thenReturn(message);
        when(buttonEvent.reply()).thenReturn(InteractionApplicationCommandCallbackReplyMono.of(buttonEvent));
        when(buttonEvent.reply(any(InteractionApplicationCommandCallbackSpec.class))).thenReturn(Mono.empty());

        // Act
        Mono<?> actual = underTest.handle(buttonEvent);

        // Assert
        StepVerifier.create(actual)
                .expectSubscription()
                .verifyComplete();
        verify(eventSignupService, times(1)).applySignup("12345", "999", "player", 1);
        verify(messageSource, times(1)).getMessage(messageKey, null, Locale.ENGLISH);
        verify(buttonEvent, times(1)).reply(Mockito.<InteractionApplicationCommandCallbackSpec>argThat(spec ->
                spec.ephemeral().toOptional().orElse(false)
                        && spec.content().toOptional().orElse("").equals(message)
        ));
        verifyNoInteractions(embedGenerator);
    }

    @Test
    void getName_ReturnsEventSignup() {
        assertThat(underTest.getName()).isEqualTo("event-signup");
    }

    private void stubSignupRequest() {
        when(buttonEvent.getCustomId()).thenReturn("12345,1");
        when(buttonEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(Snowflake.of("999"));
        when(user.getUsername()).thenReturn("player");
    }

    private EventSignupResult signupResult(EventSignupResult.Outcome outcome, Event event) {
        return switch (outcome) {
            case ADDED -> EventSignupResult.added(event);
            case UPDATED -> EventSignupResult.updated(event);
            case WAITLISTED -> EventSignupResult.waitlisted(event);
            default -> throw new IllegalArgumentException("Unexpected success outcome: " + outcome);
        };
    }

    private String messageKeyFor(EventSignupResult.Outcome outcome) {
        return switch (outcome) {
            case EVENT_FULL -> "interaction.signup.event-full";
            case EVENT_NOT_FOUND -> "interaction.signup.event-not-found";
            case ROLE_NOT_FOUND -> "interaction.signup.role-not-found";
            case INVALID_CAPACITY -> "interaction.signup.invalid-capacity";
            case EVENT_CLOSED -> "interaction.signup.event-closed";
            case EVENT_CANCELLED -> "interaction.signup.event-cancelled";
            case EVENT_EXPIRED -> "interaction.signup.event-expired";
            default -> throw new IllegalArgumentException("Unexpected blocked outcome: " + outcome);
        };
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
