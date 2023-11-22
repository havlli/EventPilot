package com.github.havlli.EventPilot.entity.user;

import jakarta.persistence.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 25)
    private String username;

    @NotBlank
    @Email
    @Size(min = 4, max = 50)
    private String email;

    @NotBlank
    @Size(min = 8, max = 120)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<UserRole> roles = new HashSet<>();

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(Long id, String username, String email, String password) {
        this(username, email, password);
        this.id = id;
    }

    public User(Long id, String username, String email, String password, Set<UserRole> roles) {
        this(id, username, email, password);
        this.roles = roles;
    }

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.roles = builder.roles;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username) && Objects.equals(email, user.email) && Objects.equals(password, user.password) && Objects.equals(roles, user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, password, roles);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", roles=" + roles +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private User user;
        private Long id;
        private String username;
        private String email;
        private String password;
        private Set<UserRole> roles;

        private Builder() { }

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withRoles(Set<UserRole> roles) {
            this.roles = roles;
            return this;
        }

        public Builder withRoles(UserRole ...roles) {
            this.roles = Set.of(roles);
            return this;
        }

        public Builder fromUser(User user) {
            this.id = user.id;
            this.username = user.username;
            this.password = user.password;
            this.email = user.email;
            this.roles = user.roles;

            return this;
        }

        public User build() {
            this.user = new User(this);
            return user;
        }

        public User getUser() {
            if (this.user == null) throw new IllegalStateException("Cannot retrieve user that was not built yet!");
            return this.user;
        }
    }
}
