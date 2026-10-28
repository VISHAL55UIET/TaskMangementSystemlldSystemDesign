package com.TaskManageMentSystem.TaskManageMentSystem.service;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Permission;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.UnauthorizedOperationException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;

/** Central place for every "is this user allowed?" decision (RBAC + ownership). */
public class AuthorizationService {

    public void require(User actor, Permission permission) {
        if (!actor.isActive() || !actor.hasPermission(permission)) {
            throw new UnauthorizedOperationException(actor.getId(), permission.name());
        }
    }

    /** Managers/admins can modify any task; members only tasks they own or created. */
    public void requireCanModify(User actor, Task task) {
        if (!actor.isActive()) {
            throw new UnauthorizedOperationException(actor.getId(), "modify " + task.getId() + " (inactive user)");
        }
        if (actor.hasPermission(Permission.UPDATE_ANY_TASK)) {
            return;
        }
        boolean isOwner = actor.getId().equals(task.getAssigneeId()) || actor.getId().equals(task.getCreatorId());
        if (actor.hasPermission(Permission.UPDATE_OWN_TASK) && isOwner) {
            return;
        }
        throw new UnauthorizedOperationException(actor.getId(), "modify " + task.getId());
    }
}
