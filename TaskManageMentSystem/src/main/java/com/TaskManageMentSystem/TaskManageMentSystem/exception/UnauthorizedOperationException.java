package com.TaskManageMentSystem.TaskManageMentSystem.exception;

public class UnauthorizedOperationException extends TaskManagementException {

    public UnauthorizedOperationException(String userId, String action) {
        super(String.format("User %s is not allowed to: %s", userId, action));
    }
}
