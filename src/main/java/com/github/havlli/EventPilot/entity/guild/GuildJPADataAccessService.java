package com.github.havlli.EventPilot.entity.guild;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GuildJPADataAccessService implements GuildDAO {

    private final GuildRepository guildRepository;

    public GuildJPADataAccessService(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    @Override
    public Optional<Guild> getGuildById(String id) {
        return guildRepository.findById(id);
    }

    @Override
    public void saveGuildIfNotExists(String guildId, String guildName) {
        guildRepository.saveGuildIfNotExists(guildId, guildName);
    }

}
