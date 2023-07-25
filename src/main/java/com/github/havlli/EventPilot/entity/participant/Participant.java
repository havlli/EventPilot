package com.github.havlli.EventPilot.entity.participant;

import com.github.havlli.EventPilot.entity.event.Event;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "participant")
public class Participant {
        private String id;
        private String username;
        private Integer position;
        private Integer roleIndex;

        @ManyToOne
        @JoinColumn(name = "event_id")
        private Event event;

        public Participant() {
        }

        public Participant(
                String id,
                String username,
                Integer position,
                Integer roleIndex
        ) {
                this.id = id;
                this.username = username;
                this.position = position;
                this.roleIndex = roleIndex;
        }

        public String getId() {
                return id;
        }

        public void setId(String id) {
                this.id = id;
        }

        public String getUsername() {
                return username;
        }

        public void setUsername(String username) {
                this.username = username;
        }

        public Integer getPosition() {
                return position;
        }

        public void setPosition(Integer position) {
                this.position = position;
        }

        public Integer getRoleIndex() {
                return roleIndex;
        }

        public void setRoleIndex(Integer roleIndex) {
                this.roleIndex = roleIndex;
        }

        @Override
        public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                var that = (Participant) obj;
                return Objects.equals(this.id, that.id) &&
                        Objects.equals(this.username, that.username) &&
                        Objects.equals(this.position, that.position) &&
                        Objects.equals(this.roleIndex, that.roleIndex);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id, username, position, roleIndex);
        }

        @Override
        public String toString() {
                return "Participant[" +
                        "userId=" + id + ", " +
                        "username=" + username + ", " +
                        "position=" + position + ", " +
                        "roleId=" + roleIndex + ']';
        }
}
