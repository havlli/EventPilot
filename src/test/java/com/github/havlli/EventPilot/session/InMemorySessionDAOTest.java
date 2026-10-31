package com.github.havlli.EventPilot.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionDAOTest {

    private final InMemorySessionDAO underTest = new InMemorySessionDAO();

    @Test
    void save_ThenExists_ReturnsTrue() {
        underTest.save("user", "name");
        assertTrue(underTest.exists("user"));
    }

    @Test
    void exists_ReturnsFalse_WhenMissing() {
        assertFalse(underTest.exists("user"));
    }

    @Test
    void remove_DeletesKey() {
        underTest.save("user", "name");
        underTest.remove("user");
        assertFalse(underTest.exists("user"));
    }

    @Test
    void clear_DeletesAllKeys() {
        underTest.save("a", "1");
        underTest.save("b", "2");
        underTest.clear();
        assertFalse(underTest.exists("a"));
        assertFalse(underTest.exists("b"));
    }
}
