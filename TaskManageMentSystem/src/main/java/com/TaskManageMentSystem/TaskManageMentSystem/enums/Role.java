package com.TaskManageMentSystem.TaskManageMentSystem.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Role-based access control: a role is just a named bundle of permissions. */
public enum Role {
    ADMIN(EnumSet.allOf(Permission.class)),
    MANAGER(EnumSet.of(
            Permission.CREATE_TASK, Permission.ASSIGN_TASK, Permission.UPDATE_ANY_TASK,
            Permission.UPDATE_OWN_TASK, Permission.COMMENT, Permission.VIEW_REPORTS)),
    MEMBER(EnumSet.of(Permission.CREATE_TASK, Permission.UPDATE_OWN_TASK, Permission.COMMENT)),
    VIEWER(EnumSet.noneOf(Permission.class));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }
}
