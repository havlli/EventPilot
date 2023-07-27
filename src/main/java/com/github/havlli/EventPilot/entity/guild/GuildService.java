package com.github.havlli.EventPilot.entity.guild;

import com.github.havlli.EventPilot.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GuildService {

    private final GuildDAO guildDAO;

    public GuildService(GuildDAO guildDAO) {
        this.guildDAO = guildDAO;
    }

    public void saveGuild(Guild guild) {
        guildDAO.insertGuild(guild);
    }

    public List<Guild> getAllGuilds() {
        return guildDAO.selectAllGuilds();
    }

    public Guild getGuildById(String id) {
        return guildDAO.selectGuildById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guild with id {%s} was not found!".formatted(id)));
    }

    public boolean existsGuildById(String id) {
        return guildDAO.existsGuildById(id);
    }

    public void createGuildIfNotExists(String id, String name) {
        if (!existsGuildById(id)) {
            Guild newGuild = new Guild(id, name);
            saveGuild(newGuild);
        }
    }

}
