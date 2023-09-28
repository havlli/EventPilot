package com.github.havlli.EventPilot.session;

import org.springframework.stereotype.Component;

@Component
public class SessionStorage {

    private final SessionDAO sessionDAO;

    public SessionStorage(SessionDAO sessionDAO) {
        this.sessionDAO = sessionDAO;
    }

    public void save(String key, String value) {
        sessionDAO.save(key, value);
    }

    public boolean exists(String key) {
        return sessionDAO.exists(key);
    }

    public void remove(String key) {
        sessionDAO.remove(key);
    }

    public void clear() {
        sessionDAO.clear();
    }
}
