package com.github.havlli.EventPilot.entity.embedtype;

import com.github.havlli.EventPilot.entity.event.Event;
import jakarta.persistence.*;

import java.util.ArrayList;
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

    public static EmbedType.Builder builder() {
        return new EmbedType.Builder();
    }

    public static class Builder {
        private String name;
        private String structure;
        private EmbedType embedType;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withStructure(String structure) {
            this.structure = structure;
            return this;
        }

        public EmbedType build() {
            validateRequiredFields();
            this.embedType = new EmbedType(name, structure, new ArrayList<>());
            return embedType;
        }

        public EmbedType getEmbedType() {
            if (embedType == null) throw new IllegalStateException("Cannot retrieve event that was not built yet!");
            return embedType;
        }

        private void validateRequiredFields() {
            Objects.requireNonNull(name, "name is required");
            Objects.requireNonNull(structure, "structure is required");
        }
    }
}
