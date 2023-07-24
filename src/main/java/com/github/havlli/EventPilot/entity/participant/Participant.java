package com.github.havlli.EventPilot.entity.participant;

import java.util.Objects;

public class Participant {
        private String userId;
        private String username;
        private Integer position;
        private Integer roleIndex;

        public Participant(
                String userId,
                String username,
                Integer position,
                Integer roleIndex
        ) {
                this.userId = userId;
                this.username = username;
                this.position = position;
                this.roleIndex = roleIndex;
        }

        public String getUserId() {
                return userId;
        }

        public void setUserId(String userId) {
                this.userId = userId;
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
                return Objects.equals(this.userId, that.userId) &&
                        Objects.equals(this.username, that.username) &&
                        Objects.equals(this.position, that.position) &&
                        Objects.equals(this.roleIndex, that.roleIndex);
        }

        @Override
        public int hashCode() {
                return Objects.hash(userId, username, position, roleIndex);
        }

        @Override
        public String toString() {
                return "Participant[" +
                        "userId=" + userId + ", " +
                        "username=" + username + ", " +
                        "position=" + position + ", " +
                        "roleId=" + roleIndex + ']';
        }
}
