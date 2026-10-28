package com.TaskManageMentSystem.TaskManageMentSystem.model;

import java.time.Instant;

public record Comment(String id, String authorId, String text, Instant createdAt) {
}
