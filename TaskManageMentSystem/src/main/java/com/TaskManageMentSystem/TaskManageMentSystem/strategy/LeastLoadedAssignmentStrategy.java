package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Picks the candidate with the fewest open tasks (ties: alphabetical by name). */
public class LeastLoadedAssignmentStrategy implements AssignmentStrategy {

    @Override
    public User select(Task task, List<User> candidates, Map<String, Long> openTaskCounts) {
        return candidates.stream()
                .min(Comparator.<User>comparingLong(u -> openTaskCounts.getOrDefault(u.getId(), 0L))
                        .thenComparing(User::getName))
                .orElseThrow(() -> new ValidationException("No eligible assignees available"));
    }
}
