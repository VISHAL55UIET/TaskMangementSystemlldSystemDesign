package com.TaskManageMentSystem.TaskManageMentSystem.model;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;

import java.time.Instant;

public record Notification(String userId, String taskId, TaskEventType type,
                           String message, Instant createdAt) {
}
