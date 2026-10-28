package com.TaskManageMentSystem.TaskManageMentSystem.exception;

/** Root of all domain errors, so callers can catch one type at API boundaries. */
public class TaskManagementException extends RuntimeException {

    public TaskManagementException(String message) {
        super(message);
    }

    public TaskManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
