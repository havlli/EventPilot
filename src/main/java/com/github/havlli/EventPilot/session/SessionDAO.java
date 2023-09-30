package com.github.havlli.EventPilot.session;

public interface SessionDAO {
    void save(String key, String value);
    Boolean exists(String key);
    void remove(String key);
    void clear();

}
