package com.TaskManageMentSystem.TaskManageMentSystem.exception;

public class CyclicDependencyException extends TaskManagementException {

    public CyclicDependencyException(String taskId, String dependsOnId) {
        super(String.format("Adding dependency %s -> %s would create a cycle", taskId, dependsOnId));
    }
}
