package com.TaskManageMentSystem.TaskManageMentSystem.model;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.InvalidStatusTransitionException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException;
import com.TaskManageMentSystem.TaskManageMentSystem.util.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
public class Task {

    private final String id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private String assigneeId;
    private final String creatorId;
    private String parentId;
    private Instant dueDate;
    private final Set<String> tags;
    private final Set<String> dependencyIds;
    private final List<Comment> comments;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Task(Builder b) {
        this.id = Preconditions.requireNonBlank(b.id, "task id");
        this.title = Preconditions.requireNonBlank(b.title, "task title");
        this.creatorId = Preconditions.requireNonBlank(b.creatorId, "creatorId");
        this.createdAt = Preconditions.requireNonNull(b.createdAt, "createdAt");
        this.description = b.description;
        this.status = TaskStatus.TODO;
        this.priority = b.priority == null ? Priority.MEDIUM : b.priority;
        this.assigneeId = b.assigneeId;
        this.parentId = b.parentId;
        this.dueDate = b.dueDate;
        this.tags = new LinkedHashSet<>();
        b.tags.forEach(t -> this.tags.add(normalizeTag(t)));
        this.dependencyIds = new LinkedHashSet<>();
        this.comments = new ArrayList<>();
        this.updatedAt = b.createdAt;
        this.version = 0;
    }

    private Task(Task o) {
        this.id = o.id;
        this.title = o.title;
        this.description = o.description;
        this.status = o.status;
        this.priority = o.priority;
        this.assigneeId = o.assigneeId;
        this.creatorId = o.creatorId;
        this.parentId = o.parentId;
        this.dueDate = o.dueDate;
        this.tags = new LinkedHashSet<>(o.tags);
        this.dependencyIds = new LinkedHashSet<>(o.dependencyIds);
        this.comments = new ArrayList<>(o.comments);
        this.createdAt = o.createdAt;
        this.updatedAt = o.updatedAt;
        this.version = o.version;
    }

    public Task copy() {
        return new Task(this);
    }

    // ---------------------------------------------------------------- domain behaviour

    /** Validated transition: throws if the state machine does not allow it. */
    public void changeStatus(TaskStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(id, status, next);
        }
        this.status = next;
    }

    /** Skips the state machine. Only for undo/restore flows. */
    public void forceStatus(TaskStatus next) {
        this.status = Preconditions.requireNonNull(next, "status");
    }

    public void assignTo(String userId) {
        this.assigneeId = userId;
    }

    public void changePriority(Priority next) {
        this.priority = Preconditions.requireNonNull(next, "priority");
    }

    public void changeDueDate(Instant due) {
        this.dueDate = due;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void addTag(String tag) {
        tags.add(normalizeTag(tag));
    }

    public void addComment(Comment comment) {
        comments.add(Preconditions.requireNonNull(comment, "comment"));
    }

    public void addDependency(String dependsOnTaskId) {
        Preconditions.requireNonBlank(dependsOnTaskId, "dependency id");
        if (dependsOnTaskId.equals(id)) {
            throw new ValidationException("A task cannot depend on itself: " + id);
        }
        dependencyIds.add(dependsOnTaskId);
    }

    public boolean isOverdue(Instant now) {
        return dueDate != null && !status.isClosed() && dueDate.isBefore(now);
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    private static String normalizeTag(String tag) {
        return Preconditions.requireNonBlank(tag, "tag").toLowerCase();
    }

    // ---------------------------------------------------------------- getters

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public String getParentId() {
        return parentId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Set<String> getDependencyIds() {
        return Collections.unmodifiableSet(dependencyIds);
    }

    public List<Comment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    /** Persistence concern: only repositories should call this. */
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Task other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s [%s | %s | %s | v%d]", id, title, priority, status, version);
    }

    // ---------------------------------------------------------------- builder

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String title;
        private String description;
        private Priority priority;
        private String assigneeId;
        private String creatorId;
        private String parentId;
        private Instant dueDate;
        private Instant createdAt;
        private final Set<String> tags = new LinkedHashSet<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

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

        public Builder creatorId(String creatorId) {
            this.creatorId = creatorId;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder dueDate(Instant dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder tags(Collection<String> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public Task build() {
            return new Task(this);
        }
    }
}
