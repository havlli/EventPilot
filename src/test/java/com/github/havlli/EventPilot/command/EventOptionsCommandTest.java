package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.core.DiscordProperties;
import com.github.havlli.EventPilot.core.SimplePermissionValidator;
import com.github.havlli.EventPilot.entity.event.Event;
import com.github.havlli.EventPilot.entity.event.EventService;
import com.github.havlli.EventPilot.entity.event.EventSettingsResolver;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.ResourceBundleMessageSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventOptionsCommandTest {

    private AutoCloseable autoCloseable;
    private EventOptionsCommand underTest;
    @Mock
    private SimplePermissionValidator permissionChecker;
    @Mock
    private UserSessionValidator userSessionValidator;
    @Mock
    private EventService eventService;
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
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages/messages");
        underTest = new EventOptionsCommand(permissionChecker, userSessionValidator, messages, eventService,
                new EventSettingsResolver(new DiscordProperties("test", null,
                        new DiscordProperties.Scheduler(60, 90))));
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void handle_IgnoresOtherCommands() {
        when(interactionEvent.getCommandName()).thenReturn("event-info");

        StepVerifier.create(underTest.handle(interactionEvent)).verifyComplete();
        verifyNoInteractions(permissionChecker, userSessionValidator, eventService);
    }

    @Test
    void handle_PermissionDeniedDoesNotReadOrChangeEvent() {
        stubHandle();
        when(permissionChecker.followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS)))
                .thenReturn(Mono.just(followupMessage));

        StepVerifier.create(underTest.handle(interactionEvent).cast(Message.class)).expectNext(followupMessage).verifyComplete();

        verify(permissionChecker).followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS));
        verifyNoInteractions(eventService);
        verify(interactionEvent, never()).getOption(anyString());
    }

    @Test
    void processInteraction_GuildOnly() {
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.empty());
        stubFollowup();

        verifyReply(underTest.processInteraction(interactionEvent), "This command can only be used in a Discord server.");
        verifyNoInteractions(eventService);
    }

    @Test
    void processInteraction_UnknownMessageId() {
        stubGuildInteraction();
        when(eventService.getEventByIdForGuild("123", "111")).thenReturn(Optional.empty());

        verifyReply(underTest.processInteraction(interactionEvent), "Event not found in this server.");
        verify(eventService, never()).updateReminderLeadMinutesForGuild(any(), any(), any());
    }

    @Test
    void handle_SetsOverrideThroughPermissionAndSessionValidation() {
        stubHandle();
        stubGuildInteraction();
        option("reminder-minutes", 4, "120");
        Event event = Event.builder().withReminderLeadMinutes(120).build();
        when(eventService.updateReminderLeadMinutesForGuild("123", "111", 120)).thenReturn(Optional.of(event));

        verifyReply(underTest.handle(interactionEvent),
                "Event options saved.\nReminder lead: 120 minutes (event override). Reminder: not sent.");
        verify(userSessionValidator).validateThenWrap(any(), eq(interactionEvent));
        verify(permissionChecker).followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS));
        verify(eventService).updateReminderLeadMinutesForGuild("123", "111", 120);
        verify(eventService, never()).getEventByIdForGuild(any(), any());
    }

    @Test
    void processInteraction_ResetUsesInstanceDefault() {
        stubGuildInteraction();
        option("reset", 5, "true");
        Event event = Event.builder().build();
        when(eventService.updateReminderLeadMinutesForGuild("123", "111", null)).thenReturn(Optional.of(event));

        verifyReply(underTest.processInteraction(interactionEvent),
                "Event options saved.\nReminder lead: 90 minutes (instance default). Reminder: not sent.");
        verify(eventService).updateReminderLeadMinutesForGuild("123", "111", null);
    }

    @Test
    void processInteraction_ShowUsesInstanceDefaultWithoutSaving() {
        stubGuildInteraction();
        when(eventService.getEventByIdForGuild("123", "111")).thenReturn(Optional.of(Event.builder().build()));

        verifyReply(underTest.processInteraction(interactionEvent),
                "Reminder lead: 90 minutes (instance default). Reminder: not sent.");
        verify(eventService, never()).updateReminderLeadMinutesForGuild(any(), any(), any());
    }

    @Test
    void processInteraction_ShowOverrideAndAlreadySentState() {
        stubGuildInteraction();
        Event event = Event.builder().withReminderLeadMinutes(15).withReminderSent(true).build();
        when(eventService.getEventByIdForGuild("123", "111")).thenReturn(Optional.of(event));

        verifyReply(underTest.processInteraction(interactionEvent),
                "Reminder lead: 15 minutes (event override). Reminder: already sent.");
        verify(eventService, never()).updateReminderLeadMinutesForGuild(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void processInteraction_ChangingAlreadySentReminderAddsNote(boolean reset) {
        stubGuildInteraction();
        if (reset) {
            option("reset", 5, "true");
        } else {
            option("reminder-minutes", 4, "15");
        }
        Event event = Event.builder().withReminderLeadMinutes(reset ? null : 15).withReminderSent(true).build();
        when(eventService.updateReminderLeadMinutesForGuild("123", "111", reset ? null : 15))
                .thenReturn(Optional.of(event));

        verifyReply(underTest.processInteraction(interactionEvent), "Event options saved.\nReminder lead: "
                + (reset ? "90 minutes (instance default)" : "15 minutes (event override)")
                + ". Reminder: already sent.\nThe reminder was already sent; this change will not trigger another reminder.");
        assertThat(event.isReminderSent()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 10081, 2147483648L, Long.MAX_VALUE})
    void processInteraction_OutOfRangeDoesNotCallService(long minutes) {
        stubGuildInteraction();
        option("reminder-minutes", 4, Long.toString(minutes));

        verifyReply(underTest.processInteraction(interactionEvent), "Reminder minutes must be between 1 and 10080.");
        verifyNoInteractions(eventService);
    }

    @Test
    void processInteraction_ConflictingOptionsDoNotCallService() {
        stubGuildInteraction();
        option("reminder-minutes", 4, "60");
        option("reset", 5, "true");

        verifyReply(underTest.processInteraction(interactionEvent), "Use either reminder-minutes or reset:true, not both.");
        verifyNoInteractions(eventService);
    }

    @Test
    void processInteraction_ResetFalseAllowsSettingMinutes() {
        stubGuildInteraction();
        option("reset", 5, "false");
        option("reminder-minutes", 4, "1");
        when(eventService.updateReminderLeadMinutesForGuild("123", "111", 1))
                .thenReturn(Optional.of(Event.builder().withReminderLeadMinutes(1).build()));

        verifyReply(underTest.processInteraction(interactionEvent),
                "Event options saved.\nReminder lead: 1 minutes (event override). Reminder: not sent.");
    }

    @Test
    void processInteraction_RejectsUpdateForMissingOrInactiveOrOtherGuildEvent() {
        stubGuildInteraction();
        option("reminder-minutes", 4, "60");
        when(eventService.updateReminderLeadMinutesForGuild("123", "111", 60)).thenReturn(Optional.empty());

        verifyReply(underTest.processInteraction(interactionEvent), "Open or closed event not found in this server.");
    }

    private void stubHandle() {
        when(interactionEvent.getCommandName()).thenReturn("event-options");
        InteractionCallbackSpecDeferReplyMono defer = mock(InteractionCallbackSpecDeferReplyMono.class);
        when(interactionEvent.deferReply()).thenReturn(defer);
        when(defer.withEphemeral(true)).thenReturn(defer);
        when(defer.then(org.mockito.ArgumentMatchers.<Mono<Message>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionValidator.validateThenWrap(any(), eq(interactionEvent)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionChecker.followupWith(any(), eq(interactionEvent), eq(Permission.MANAGE_CHANNELS)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubGuildInteraction() {
        when(interactionEvent.getInteraction()).thenReturn(interaction);
        when(interaction.getGuildId()).thenReturn(Optional.of(Snowflake.of("111")));
        option("message-id", 3, "123");
        stubFollowup();
    }

    private void stubFollowup() {
        when(interactionEvent.createFollowup(followupSpecCaptor.capture())).thenReturn(Mono.just(followupMessage));
    }

    private void verifyReply(Mono<?> response, String content) {
        StepVerifier.create(response.cast(Message.class)).expectNext(followupMessage).verifyComplete();
        InteractionFollowupCreateSpec spec = followupSpecCaptor.getValue();
        assertThat(spec.contentOrElse("")).isEqualTo(content);
        assertThat(spec.ephemeralOrElse(false)).isTrue();
    }

    private void option(String name, int type, String value) {
        ApplicationCommandInteractionOptionData data = ApplicationCommandInteractionOptionData.builder()
                .name(name).type(type).value(value).build();
        when(interactionEvent.getOption(name))
                .thenReturn(Optional.of(new ApplicationCommandInteractionOption(client, data, 111L, null)));
    }
}
