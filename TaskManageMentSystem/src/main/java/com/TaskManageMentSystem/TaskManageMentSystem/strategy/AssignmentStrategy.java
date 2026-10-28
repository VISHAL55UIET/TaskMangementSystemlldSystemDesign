package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;

import java.util.List;
import java.util.Map;

/** Strategy pattern: how an assignee is picked automatically. */
public interface AssignmentStrategy {

    /**
     * @param openTaskCounts number of open tasks per user id (missing key = 0)
     * @throws com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException if candidates is empty
     */
    User select(Task task, List<User> candidates, Map<String, Long> openTaskCounts);
}
