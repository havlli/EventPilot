package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventStatus;
import com.github.havlli.EventPilot.session.UserSessionValidator;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionCallbackSpecDeferReplyMono;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.rest.util.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ListEventsCommandTest {

    private AutoCloseable autoCloseable;
    private ListEventsCommand underTest;
    @Mock
    private SimplePermissionValidator permissionChecker;
    @Mock
    private UserSessionValidator userSessionValidator;
    @Mock
    private MessageSource messageSource;
    @Mock
    private EventService eventService;
    @Mock
    private OrganizerEventFormatter organizerEventFormatter;
    @Mock
    private ChatInputInteractionEvent interactionEvent;
    @Mock
    private Interaction interaction;
    @Mock
    private GatewayDiscordClient client;
    @Mock
    private Message followupMessage;
    @Captor
    private ArgumentCaptor<InteractionFollowupCreateSpec> followupSpecCaptor;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new ListEventsCommand(
                permissionChecker,
                userSessionValidator,
                messageSource,
                eventService,
                organizerEventFormatter
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void listEventsInteraction_UsesActiveFilterAndDefaultLimit_WhenOptionsAreMissing() {
        // Arrange
        Event event = mock(Event.class);
        stubGuildInteraction();
        when(interactionEvent.getOption("status")).thenReturn(Optional.empty());
        when(interactionEvent.getOption("limit")).thenReturn(Optional.empty());
        when(eventService.getEventsForGuild("111", List.of(EventStatus.OPEN, EventStatus.CLOSED), 5))
                .thenReturn(List.of(event));
        when(organizerEventFormatter.formatEventList(List.of(event))).thenReturn("Events:\n- Raid");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.listEventsInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("Events:\n- Raid");
    }

    @Test
    void listEventsInteraction_UsesAllFilterAndClampedLimit_WhenOptionsArePresent() {
        // Arrange
        Event event = mock(Event.class);
        stubGuildInteraction();
        when(interactionEvent.getOption("status")).thenReturn(Optional.of(stringOption("all")));
        when(interactionEvent.getOption("limit")).thenReturn(Optional.of(longOption(25)));
        when(eventService.getEventsForGuild("111", List.of(), 10)).thenReturn(List.of(event));
        when(organizerEventFormatter.formatEventList(List.of(event))).thenReturn("Events:\n- Raid");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.listEventsInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("Events:\n- Raid");
    }

    @Test
    void listEventsInteraction_RepliesNoEvents_WhenNoMatchingEventsExist() {
        // Arrange
        stubGuildInteraction();
        when(interactionEvent.getOption("status")).thenReturn(Optional.empty());
        when(interactionEvent.getOption("limit")).thenReturn(Optional.empty());
        when(eventService.getEventsForGuild("111", List.of(EventStatus.OPEN, EventStatus.CLOSED), 5))
                .thenReturn(List.of());
        when(messageSource.getMessage("interaction.list-events.no-events", null, Locale.ENGLISH))
                .thenReturn("No matching events found in this server.");
        stubFollowup();

        // Act
        Mono<Message> actual = underTest.listEventsInteraction(interactionEvent);

        // Assert
        StepVerifier.create(actual)
                .expectNext(followupMessage)
                .verifyComplete();
        assertFollowup("No matching events found in this server.");
    }

    @Test
    void handle_ReturnsPermissionDeniedMessageAndDoesNotQueryEvents_WhenPermissionValidatorRejects() {
        // Arrange
        Message deniedMessage = mock(Message.class);
        stubValidatedHandle();
        when(permissionChecker.followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS)))
                .thenReturn(Mono.just(deniedMessage));

        // Act
        Mono<Message> actual = underTest.handle(interactionEvent)
                .cast(Message.class);

        // Assert
        StepVerifier.create(actual)
                .expectNext(deniedMessage)
                .verifyComplete();
        verify(eventService, never()).getEventsForGuild(any(), any(), anyInt());
    }

    private void stubValidatedHandle() {
        when(interactionEvent.getCommandName()).thenReturn("list-events");
        stubGuildInteraction();
        when(interactionEvent.getOption("status")).thenReturn(Optional.empty());
        when(interactionEvent.getOption("limit")).thenReturn(Optional.empty());
        InteractionCallbackSpecDeferReplyMono deferReplyMono = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(deferReplyMono);
        when(deferReplyMono.withEphemeral(true)).thenReturn(deferReplyMono);
        when(deferReplyMono.then(org.mockito.ArgumentMatchers.<Mono<Message>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionValidator.validateThenWrap(any(), eq(interactionEvent)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubGuildInteraction() {
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.of(Snowflake.of("111")));
    }

    private void stubFollowup() {
        when(interactionEvent.createFollowup(followupSpecCaptor.capture())).thenReturn(Mono.just(followupMessage));
    }

    private void assertFollowup(String expectedContent) {
        InteractionFollowupCreateSpec spec = followupSpecCaptor.getValue();
        assertThat(spec.contentOrElse("")).isEqualTo(expectedContent);
        assertThat(spec.ephemeralOrElse(false)).isTrue();
    }

    private ApplicationCommandInteractionOption stringOption(String rawValue) {
        return option("status", 3, rawValue);
    }

    private ApplicationCommandInteractionOption longOption(long rawValue) {
        return option("limit", 4, String.valueOf(rawValue));
    }

    private ApplicationCommandInteractionOption option(String name, int type, String rawValue) {
        ApplicationCommandInteractionOptionData data = ApplicationCommandInteractionOptionData.builder()
                .name(name)
                .type(type)
                .value(rawValue)
                .build();
        return new ApplicationCommandInteractionOption(client, data, 111L, null);
    }
}
