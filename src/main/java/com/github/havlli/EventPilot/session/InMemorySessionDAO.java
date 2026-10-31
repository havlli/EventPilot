package com.github.havlli.EventPilot.session;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySessionDAO implements SessionDAO {

    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(String key, String value) {
        sessions.put(key, value);
    }

    @Override
    public Boolean exists(String key) {
        return sessions.containsKey(key);
    }

    @Override
    public void remove(String key) {
        sessions.remove(key);
    }

    @Override
    public void clear() {
        sessions.clear();
    }
}
