package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.time.Instant;
import java.util.Comparator;

/** Newest first. */
public class CreatedAtSortStrategy implements SortStrategy {

    @Override
    public Comparator<Task> comparator() {
        return Comparator.comparing(Task::getCreatedAt, Comparator.<Instant>reverseOrder())
                .thenComparing(Task::getId, Comparator.<String>reverseOrder());
    }

    @Override
    public String name() {
        return "newest-first";
    }
}
