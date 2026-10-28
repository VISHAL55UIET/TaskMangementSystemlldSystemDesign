package com.TaskManageMentSystem.TaskManageMentSystem.dto;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.time.Instant;
import java.util.Set;

/**
 * Immutable message sent from the services to every observer.
 * {@code task} is a snapshot taken at the moment of the event.
 */
public record TaskEvent(TaskEventType type, Task task, String actorId, String message,
                        Set<String> recipientIds, Instant occurredAt) {
}
