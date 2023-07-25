package com.github.havlli.EventPilot.entity.guild;

import java.util.List;
import java.util.Optional;

public interface GuildDAO {
    Optional<Guild> selectGuildById(String id);
    List<Guild> selectAllGuilds();
    boolean existsGuildById(String id);
    void insertGuild(Guild guild);
}
