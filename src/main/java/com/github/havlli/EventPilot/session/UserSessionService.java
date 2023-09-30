package com.github.havlli.EventPilot.session;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserSessionService {
    private final SessionStorage sessionStorage;

    public UserSessionService(SessionStorage sessionStorage) {
        this.sessionStorage = sessionStorage;
    }

    @PostConstruct
    protected void clearDatabaseOnInitialization() {
        sessionStorage.clear();
    }

    public Optional<UserSession> createUserSession(String userId, String username) {
        if (sessionStorage.exists(userId)) {
            return Optional.empty();
        }
        sessionStorage.save(userId, username);
        return Optional.of(new UserSession(userId, username));
    }

    public void terminateUserSession(String userId) {
        sessionStorage.remove(userId);
    }
}
