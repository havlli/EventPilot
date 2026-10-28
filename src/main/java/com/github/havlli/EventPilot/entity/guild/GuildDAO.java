package com.github.havlli.EventPilot.entity.guild;

import java.util.Optional;

public interface GuildDAO {
    Optional<Guild> getGuildById(String id);
    void saveGuildIfNotExists(String id, String name);
}
