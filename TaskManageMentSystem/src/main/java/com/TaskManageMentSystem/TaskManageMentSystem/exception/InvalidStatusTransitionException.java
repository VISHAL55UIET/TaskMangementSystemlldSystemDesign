package com.TaskManageMentSystem.TaskManageMentSystem.exception;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;

public class InvalidStatusTransitionException extends TaskManagementException {

    public InvalidStatusTransitionException(String taskId, TaskStatus from, TaskStatus to) {
        super(String.format("Task %s: cannot move from %s to %s (allowed: %s)",
                taskId, from, to, from.allowedTransitions()));
    }
}
