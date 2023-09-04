package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "username")
    private String username;
    @Column(name = "position")
    private Integer position;
    @Column(name = "role_index")
    private Integer roleIndex;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    public Participant() {
    }

    public Participant(
        Long id,
        String userId,
        String username,
        Integer position,
        Integer roleIndex,
        Event event
    ) {
        this(userId, username, position, roleIndex, event);
        this.id = id;
    }

    public Participant(
        String userId,
        String username,
        Integer position,
        Integer roleIndex,
        Event event
    ) {
        this.userId = userId;
        this.username = username;
        this.position = position;
        this.roleIndex = roleIndex;
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getRoleIndex() {
        return roleIndex;
    }

    public void setRoleIndex(Integer roleIndex) {
        this.roleIndex = roleIndex;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId) && Objects.equals(username, that.username) && Objects.equals(position, that.position) && Objects.equals(roleIndex, that.roleIndex) && Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, username, position, roleIndex, event);
    }

    @Override
    public String toString() {
        return "Participant{" +
            "id=" + id +
            ", userId='" + userId + '\'' +
            ", username='" + username + '\'' +
            ", position=" + position +
            ", roleIndex=" + roleIndex +
            ", event=" + event.getEventId() +
            '}';
    }
}
