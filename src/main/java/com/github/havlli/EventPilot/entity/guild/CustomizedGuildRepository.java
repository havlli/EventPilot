package com.github.havlli.EventPilot.entity.guild;

public interface CustomizedGuildRepository {
    void saveGuildIfNotExists(String id, String name);
}
