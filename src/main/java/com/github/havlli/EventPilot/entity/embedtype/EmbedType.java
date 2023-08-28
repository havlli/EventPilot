package com.github.havlli.EventPilot.entity.embedtype;

import com.github.havlli.EventPilot.entity.event.Event;
import jakarta.persistence.*;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "embed_type")
public class EmbedType {
    @Id
    @Column(name = "id")
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "structure")
    private String structure;
    @OneToMany(mappedBy = "embedType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events;

    public EmbedType(Integer id, String name, String structure, List<Event> events) {
        this.id = id;
        this.name = name;
        this.structure = structure;
        this.events = events;
    }

    public EmbedType() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
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
        EmbedType embedType = (EmbedType) o;
        return Objects.equals(id, embedType.id) && Objects.equals(name, embedType.name) && Objects.equals(structure, embedType.structure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, structure);
    }

    @Override
    public String toString() {
        return "EmbedType{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", structure='" + structure + '\'' +
                '}';
    }
}
