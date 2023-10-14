package com.github.havlli.EventPilot.entity.user;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "role")
public class UserRole {
    public enum Role {
        USER,
        MODERATOR,
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name")
    private Role role;

    public UserRole() {}

    public UserRole(Role role) {
        this.role = role;
    }

    public UserRole(String roleName) {
        this.role = Role.valueOf(roleName);
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRole userRole = (UserRole) o;
        return Objects.equals(id, userRole.id) && role == userRole.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, role);
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", role=" + role +
                '}';
    }
}
