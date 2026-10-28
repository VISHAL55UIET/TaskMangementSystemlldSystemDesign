package com.TaskManageMentSystem.TaskManageMentSystem.dto;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Search criteria. Every populated field narrows the result (AND), and
 * {@link #toPredicate(Instant)} turns the whole thing into one Predicate.
 */
public final class TaskFilter {

    private final Set<TaskStatus> statuses;
    private final Set<Priority> priorities;
    private final String assigneeId;
    private final String creatorId;
    private final Set<String> tags;
    private final Instant dueBefore;
    private final Instant dueAfter;
    private final String keyword;
    private final boolean onlyOpen;
    private final boolean overdueOnly;

    private TaskFilter(Builder b) {
        this.statuses = EnumSet.copyOf(b.statuses.isEmpty() ? EnumSet.noneOf(TaskStatus.class) : b.statuses);
        this.priorities = EnumSet.copyOf(b.priorities.isEmpty() ? EnumSet.noneOf(Priority.class) : b.priorities);
        this.assigneeId = b.assigneeId;
        this.creatorId = b.creatorId;
        this.tags = new LinkedHashSet<>(b.tags);
        this.dueBefore = b.dueBefore;
        this.dueAfter = b.dueAfter;
        this.keyword = b.keyword == null ? null : b.keyword.toLowerCase(Locale.ROOT);
        this.onlyOpen = b.onlyOpen;
        this.overdueOnly = b.overdueOnly;
    }

    public static TaskFilter none() {
        return builder().build();
    }

    public Predicate<Task> toPredicate(Instant now) {
        Predicate<Task> p = t -> true;
        if (!statuses.isEmpty()) {
            p = p.and(t -> statuses.contains(t.getStatus()));
        }
        if (!priorities.isEmpty()) {
            p = p.and(t -> priorities.contains(t.getPriority()));
        }
        if (assigneeId != null) {
            p = p.and(t -> assigneeId.equals(t.getAssigneeId()));
        }
        if (creatorId != null) {
            p = p.and(t -> creatorId.equals(t.getCreatorId()));
        }
        if (!tags.isEmpty()) {
            p = p.and(t -> tags.stream().anyMatch(t.getTags()::contains));
        }
        if (dueBefore != null) {
            p = p.and(t -> t.getDueDate() != null && t.getDueDate().isBefore(dueBefore));
        }
        if (dueAfter != null) {
            p = p.and(t -> t.getDueDate() != null && t.getDueDate().isAfter(dueAfter));
        }
        if (keyword != null && !keyword.isBlank()) {
            p = p.and(t -> contains(t.getTitle(), keyword) || contains(t.getDescription(), keyword));
        }
        if (onlyOpen) {
            p = p.and(t -> !t.getStatus().isClosed());
        }
        if (overdueOnly) {
            p = p.and(t -> t.isOverdue(now));
        }
        return p;
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Set<TaskStatus> statuses = EnumSet.noneOf(TaskStatus.class);
        private final Set<Priority> priorities = EnumSet.noneOf(Priority.class);
        private String assigneeId;
        private String creatorId;
        private final Set<String> tags = new LinkedHashSet<>();
        private Instant dueBefore;
        private Instant dueAfter;
        private String keyword;
        private boolean onlyOpen;
        private boolean overdueOnly;

        public Builder status(TaskStatus... values) {
            statuses.addAll(Arrays.asList(values));
            return this;
        }

        public Builder priority(Priority... values) {
            priorities.addAll(Arrays.asList(values));
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

        public Builder tag(String... values) {
            for (String v : values) {
                tags.add(v.trim().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        public Builder dueBefore(Instant dueBefore) {
            this.dueBefore = dueBefore;
            return this;
        }

        public Builder dueAfter(Instant dueAfter) {
            this.dueAfter = dueAfter;
            return this;
        }

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder onlyOpen(boolean onlyOpen) {
            this.onlyOpen = onlyOpen;
            return this;
        }

        public Builder overdueOnly(boolean overdueOnly) {
            this.overdueOnly = overdueOnly;
            return this;
        }

        public TaskFilter build() {
            return new TaskFilter(this);
        }
    }
}
