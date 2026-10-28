package com.TaskManageMentSystem.TaskManageMentSystem.repository;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    /**
     * Inserts (version 0) or updates a task. Updates are rejected with an
     * OptimisticLockException when the supplied version is not the stored one.
     * Returns the stored state, including the new version.
     */
    Task save(Task task);

    Optional<Task> findById(String id);

    List<Task> findAll();

    List<Task> findByAssigneeId(String assigneeId);

    List<Task> findByParentId(String parentId);

    /** Tasks that list {@code taskId} as one of their dependencies. */
    List<Task> findDependents(String taskId);

    boolean existsById(String id);

    boolean deleteById(String id);
}
