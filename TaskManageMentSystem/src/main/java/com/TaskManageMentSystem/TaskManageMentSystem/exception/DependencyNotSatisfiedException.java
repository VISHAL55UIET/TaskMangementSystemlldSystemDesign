package com.TaskManageMentSystem.TaskManageMentSystem.exception;

import java.util.List;

public class DependencyNotSatisfiedException extends TaskManagementException {

    public DependencyNotSatisfiedException(String taskId, List<String> pendingDependencyIds) {
        super(String.format("Task %s cannot be completed, unfinished dependencies: %s",
                taskId, pendingDependencyIds));
    }
}
