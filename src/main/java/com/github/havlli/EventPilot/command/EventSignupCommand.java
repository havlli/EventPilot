package com.github.havlli.EventPilot.command;

import com.github.havlli.EventPilot.entity.event.EventSignupResult;
import com.github.havlli.EventPilot.entity.event.EventSignupService;
import com.github.havlli.EventPilot.generator.EmbedGenerator;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionReplyEditSpec;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Locale;
import java.util.Optional;

@Component
public class EventSignupCommand implements SlashCommand {

    private static final String EVENT_NAME = "event-signup";
    private static final String DELIMITER = ",";
    private final EventSignupService eventSignupService;
    private final EmbedGenerator embedGenerator;
    private final MessageSource messageSource;
    private Class<? extends Event> eventType = ButtonInteractionEvent.class;

    public EventSignupCommand(
            EventSignupService eventSignupService,
            EmbedGenerator embedGenerator,
            MessageSource messageSource
    ) {
        this.eventSignupService = eventSignupService;
        this.embedGenerator = embedGenerator;
        this.messageSource = messageSource;
    }

    @Override
    public String getName() {
        return EVENT_NAME;
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
        ButtonInteractionEvent buttonEvent = (ButtonInteractionEvent) event;
        Optional<SignupRequest> signupRequest = parseSignupRequest(buttonEvent.getCustomId());

        if (signupRequest.isEmpty()) {
            return Mono.empty();
        }

        User user = buttonEvent.getInteraction().getUser();
        SignupRequest request = signupRequest.orElseThrow();

        return Mono.fromCallable(() -> eventSignupService.applySignup(
                        request.eventId(),
                        user.getId().asString(),
                        user.getUsername(),
                        request.roleIndex()
                ))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(result -> handleSignupResult(buttonEvent, result));
    }

    private Optional<SignupRequest> parseSignupRequest(String customId) {
        String[] parts = customId.split(DELIMITER, -1);
        if (parts.length != 2 || parts[0].isBlank() || !isSnowflake(parts[0])) {
            return Optional.empty();
        }

        try {
            return Optional.of(new SignupRequest(parts[0], Integer.parseInt(parts[1])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean isSnowflake(String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    private Mono<?> handleSignupResult(ButtonInteractionEvent event, EventSignupResult result) {
        return switch (result.outcome()) {
            case ADDED, UPDATED, WAITLISTED -> updateEventMessage(event, result);
            case EVENT_FULL -> reply(event, "interaction.signup.event-full");
            case EVENT_NOT_FOUND -> reply(event, "interaction.signup.event-not-found");
            case ROLE_NOT_FOUND -> reply(event, "interaction.signup.role-not-found");
            case INVALID_CAPACITY -> reply(event, "interaction.signup.invalid-capacity");
            case EVENT_CLOSED -> reply(event, "interaction.signup.event-closed");
            case EVENT_CANCELLED -> reply(event, "interaction.signup.event-cancelled");
            case EVENT_EXPIRED -> reply(event, "interaction.signup.event-expired");
        };
    }

    private Mono<?> updateEventMessage(ButtonInteractionEvent event, EventSignupResult result) {
        return event.deferEdit()
                .then(event.editReply(InteractionReplyEditSpec.builder()
                        .addEmbed(embedGenerator.generateEmbed(result.event()))
                        .addAllComponents(embedGenerator.generateComponents(result.event()))
                        .build()));
    }

    private Mono<?> reply(ButtonInteractionEvent event, String messageKey) {
        String message = messageSource.getMessage(messageKey, null, Locale.ENGLISH);
        return event.reply()
                .withEphemeral(true)
                .withContent(message);
    }

    private record SignupRequest(String eventId, int roleIndex) {
    }
}
