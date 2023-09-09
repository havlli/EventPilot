package com.github.havlli.EventPilot.entity.guild;

import java.util.List;
import java.util.Optional;

public interface GuildDAO {
    Optional<Guild> getGuildById(String id);
    List<Guild> getGuilds();
    boolean existsGuildById(String id);
    void saveGuild(Guild guild);
}
