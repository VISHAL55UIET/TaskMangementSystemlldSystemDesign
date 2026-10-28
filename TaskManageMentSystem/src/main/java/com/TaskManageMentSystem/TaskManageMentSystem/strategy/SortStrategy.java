package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.util.Comparator;

/** Strategy pattern: how search results are ordered is pluggable. */
public interface SortStrategy {

    Comparator<Task> comparator();

    String name();
}
