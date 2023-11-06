package com.github.havlli.EventPilot.session;

import com.github.havlli.EventPilot.core.MessageCreator;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class UserSessionValidator {

    private final static Logger LOG = LoggerFactory.getLogger(UserSessionValidator.class);
    private final UserSessionService userSessionService;
    private final MessageCreator messageCreator;

    public UserSessionValidator(UserSessionService userSessionService, MessageCreator messageCreator) {
        this.userSessionService = userSessionService;
        this.messageCreator = messageCreator;
    }

    public Mono<Message> validateThenWrap(Mono<Message> followupMessage, ChatInputInteractionEvent event) {
        String userId = extractUserId(event);
        String username = extractUsername(event);
        Optional<UserSession> userSession = userSessionService.createUserSession(userId, username);
        if (userSession.isEmpty()) {
            return sessionAlreadyActiveMessage(event);
        }
        return followupMessage
                .doFinally(__ -> {
                    LOG.info("Terminating user session - %s".formatted(event.getInteraction().getUser().getUsername()));
                    terminate(event);
                });
    }

    public void terminate(ChatInputInteractionEvent event) {
        String userId = extractUserId(event);
        userSessionService.terminateUserSession(userId);
    }

    private static String extractUsername(ChatInputInteractionEvent event) {
        return event.getInteraction().getUser().getUsername();
    }

    private static String extractUserId(ChatInputInteractionEvent event) {
        return event.getInteraction().getUser().getId().asString();
    }

    private Mono<Message> sessionAlreadyActiveMessage(ChatInputInteractionEvent event) {
        return messageCreator.sessionAlreadyActive(event);
    }
}
