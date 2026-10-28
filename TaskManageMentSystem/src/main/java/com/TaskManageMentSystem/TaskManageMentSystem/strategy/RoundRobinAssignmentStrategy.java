package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Rotates through candidates in a stable (id) order, ignoring current load. */
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public User select(Task task, List<User> candidates, Map<String, Long> openTaskCounts) {
        if (candidates.isEmpty()) {
            throw new ValidationException("No eligible assignees available");
        }
        List<User> ordered = candidates.stream().sorted(Comparator.comparing(User::getId)).toList();
        return ordered.get(Math.floorMod(cursor.getAndIncrement(), ordered.size()));
    }
}
