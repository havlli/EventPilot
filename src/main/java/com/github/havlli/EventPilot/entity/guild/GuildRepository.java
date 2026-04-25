package com.github.havlli.EventPilot.entity.guild;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuildRepository extends JpaRepository<Guild, String>, CustomizedGuildRepository {
    @Override
    @EntityGraph(attributePaths = "events")
    List<Guild> findAll();

    @Override
    @EntityGraph(attributePaths = "events")
    Optional<Guild> findById(String id);
}
