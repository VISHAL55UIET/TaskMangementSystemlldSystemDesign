package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.time.Instant;
import java.util.Comparator;

/** Earliest due date first; tasks without a due date go last. */
public class DueDateSortStrategy implements SortStrategy {

    @Override
    public Comparator<Task> comparator() {
        return Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
                .thenComparing(Task::getId);
    }

    @Override
    public String name() {
        return "due-date";
    }
}
