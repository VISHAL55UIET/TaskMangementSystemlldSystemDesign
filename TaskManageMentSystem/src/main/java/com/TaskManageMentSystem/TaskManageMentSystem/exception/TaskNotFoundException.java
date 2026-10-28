package com.TaskManageMentSystem.TaskManageMentSystem.exception;

public class TaskNotFoundException extends TaskManagementException {

    public TaskNotFoundException(String taskId) {
        super("Task not found: " + taskId);
    }
}
