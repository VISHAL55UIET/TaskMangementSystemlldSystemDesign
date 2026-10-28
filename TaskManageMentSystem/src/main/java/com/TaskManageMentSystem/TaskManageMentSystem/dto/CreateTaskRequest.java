package com.TaskManageMentSystem.TaskManageMentSystem.dto;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CreateTaskRequest {

    private final String title;
    private final String description;
    private final Priority priority;
    private final String assigneeId;
    private final Instant dueDate;
    private final Set<String> tags;
    private final String parentId;

    private CreateTaskRequest(Builder b) {
        this.title = b.title;
        this.description = b.description;
        this.priority = b.priority;
        this.assigneeId = b.assigneeId;
        this.dueDate = b.dueDate;
        this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(b.tags));
        this.parentId = b.parentId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getParentId() {
        return parentId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private String description;
        private Priority priority = Priority.MEDIUM;
        private String assigneeId;
        private Instant dueDate;
        private final Set<String> tags = new LinkedHashSet<>();
        private String parentId;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder assigneeId(String assigneeId) {
            this.assigneeId = assigneeId;
            return this;
        }

        public Builder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder tags(String... tags) {
            this.tags.addAll(Arrays.asList(tags));
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public CreateTaskRequest build() {
            return new CreateTaskRequest(this);
        }
    }
}
