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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "structure")
    private String structure;
    @OneToMany(mappedBy = "embedType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Event> events;

    public EmbedType(Long id, String name, String structure, List<Event> events) {
        this(name, structure, events);
        this.id = id;
    }

    public EmbedType(String name, String structure, List<Event> events) {
        this.name = name;
        this.structure = structure;
        this.events = events;
    }

    public EmbedType() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStructure() {
        return structure;
    }

    public List<Event> getEvents() {
        return events;
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
