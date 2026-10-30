package com.github.havlli.EventPilot.entity.guild;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class GuildService {

    private final GuildDAO guildDAO;

    public GuildService(GuildDAO guildDAO) {
        this.guildDAO = guildDAO;
    }

    public Guild getGuildById(String id) {
        return guildDAO.getGuildById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guild with id {%s} was not found!".formatted(id)));
    }

    public void createGuildIfNotExists(String id, String name) {
        guildDAO.saveGuildIfNotExists(id, name);
    }

}
