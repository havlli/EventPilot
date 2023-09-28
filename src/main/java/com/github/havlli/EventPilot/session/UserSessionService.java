package com.github.havlli.EventPilot.session;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserSessionService {
    private final SessionStorage sessionRepository;

    public UserSessionService(SessionStorage sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @PostConstruct
    public void clearDatabaseOnInitialization() {
        sessionRepository.clear();
    }

    public Optional<UserSession> createUserSession(String userId, String username) {
        if (sessionRepository.exists(userId)) {
            return Optional.empty();
        }
        sessionRepository.save(userId, username);
        return Optional.of(new UserSession(userId, username));
    }

    public void terminateUserSession(UserSession userSession) {
        sessionRepository.remove(userSession.userId());
    }
}
