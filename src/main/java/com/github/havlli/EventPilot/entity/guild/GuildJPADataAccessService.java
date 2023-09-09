package com.github.havlli.EventPilot.entity.guild;

import org.springframework.stereotype.Repository;

import java.util.List;
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
    public List<Guild> getGuilds() {
        return guildRepository.findAll();
    }

    @Override
    public boolean existsGuildById(String id) {
        return guildRepository.existsById(id);
    }

    @Override
    public void saveGuild(Guild guild) {
        guildRepository.save(guild);
    }
}
