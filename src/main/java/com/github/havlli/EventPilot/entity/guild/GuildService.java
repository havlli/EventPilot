package com.github.havlli.EventPilot.entity.guild;

import org.springframework.stereotype.Service;

@Service
public class GuildService {

    private final GuildDAO guildDAO;

    public GuildService(GuildDAO guildDAO) {
        this.guildDAO = guildDAO;
    }

    public void insertGuild(Guild guild) {
        guildDAO.insertGuild(guild);
    }
}
