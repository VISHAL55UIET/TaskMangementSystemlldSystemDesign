package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.time.Instant;
import java.util.Comparator;

/** Most urgent first; ties broken by earliest due date (no due date last), then id. */
public class PrioritySortStrategy implements SortStrategy {

    @Override
    public Comparator<Task> comparator() {
        return Comparator.comparingInt((Task t) -> t.getPriority().getWeight()).reversed()
                .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
                .thenComparing(Task::getId);
    }

    @Override
    public String name() {
        return "priority";
    }
}
