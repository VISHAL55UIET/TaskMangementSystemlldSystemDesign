package com.TaskManageMentSystem.TaskManageMentSystem.exception;

public class UserNotFoundException extends TaskManagementException {

    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
