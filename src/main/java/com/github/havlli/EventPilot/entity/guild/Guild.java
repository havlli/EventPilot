package com.github.havlli.EventPilot.entity.guild;

import com.github.havlli.EventPilot.entity.event.Event;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "guild")
public class Guild {

    @Id
    @Column(nullable = false)
    private String id;
    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "guild", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events;

    public Guild() {
    }

    public Guild(String id, String name) {
        this.id = id;
        this.name = name;
        this.events = new ArrayList<>();
    }

    public Guild(String id, String name, List<Event> events) {
        this(id, name);
        this.events = events;
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

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Guild guild = (Guild) o;
        return Objects.equals(id, guild.id) && Objects.equals(name, guild.name) && Objects.equals(events, guild.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, events);
    }

    @Override
    public String toString() {
        return "Guild{" +
                "snowflakeId='" + id + '\'' +
                ", guildName='" + name + '\'' +
                '}';
    }
}
