package com.TaskManageMentSystem.TaskManageMentSystem.model;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Permission;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Role;
import com.TaskManageMentSystem.TaskManageMentSystem.util.Preconditions;

import java.util.Objects;

public class User {

    private final String id;
    private String name;
    private final String email;
    private Role role;
    private boolean active = true;

    public User(String id, String name, String email, Role role) {
        this.id = Preconditions.requireNonBlank(id, "user id");
        this.name = Preconditions.requireNonBlank(name, "user name");
        this.email = Preconditions.requireNonBlank(email, "user email").toLowerCase();
        this.role = Preconditions.requireNonNull(role, "user role");
    }

    public boolean hasPermission(Permission permission) {
        return role.has(permission);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Preconditions.requireNonBlank(name, "user name");
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = Preconditions.requireNonNull(role, "user role");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + "(" + id + ", " + role + ")";
    }
}
