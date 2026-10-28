package com.TaskManageMentSystem.TaskManageMentSystem.model;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;

import java.time.Instant;

/** Immutable audit-trail entry: who did what to which task, and when. */
public record ActivityLog(String id, String taskId, String actorId, TaskEventType type,
                          String description, Instant timestamp) {
}
