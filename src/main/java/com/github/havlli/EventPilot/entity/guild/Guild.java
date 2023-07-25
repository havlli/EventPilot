package com.github.havlli.EventPilot.entity.guild;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "guild")
public class Guild {

    @Id
    @Column(nullable = false)
    private String id;
    @Column(nullable = false)
    private String name;

    public Guild() {
    }

    public Guild(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String guildName) {
        this.name = guildName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Guild guild = (Guild) o;
        return Objects.equals(id, guild.id) && Objects.equals(name, guild.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Guild{" +
                "snowflakeId='" + id + '\'' +
                ", guildName='" + name + '\'' +
                '}';
    }
}
