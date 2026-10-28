package com.TaskManageMentSystem.TaskManageMentSystem.repository;

import com.TaskManageMentSystem.TaskManageMentSystem.exception.OptimisticLockException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.TaskNotFoundException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Thread-safe in-memory store. Stores copies and hands out copies, so callers can never
 * mutate stored state by accident. The version check runs inside ConcurrentHashMap.compute,
 * which makes check-and-write atomic per key.
 */
public class InMemoryTaskRepository implements TaskRepository {

    private final ConcurrentMap<String, Task> store = new ConcurrentHashMap<>();

    @Override
    public Task save(Task task) {
        Task stored = store.compute(task.getId(), (id, existing) -> {
            if (existing == null) {
                if (task.getVersion() != 0) {
                    throw new TaskNotFoundException(id); // updated after it was deleted
                }
            } else if (existing.getVersion() != task.getVersion()) {
                throw new OptimisticLockException(id, task.getVersion(), existing.getVersion());
            }
            Task copy = task.copy();
            copy.setVersion(task.getVersion() + 1);
            return copy;
        });
        return stored.copy();
    }

    @Override
    public Optional<Task> findById(String id) {
        return Optional.ofNullable(store.get(id)).map(Task::copy);
    }

    @Override
    public List<Task> findAll() {
        return query(t -> true);
    }

    @Override
    public List<Task> findByAssigneeId(String assigneeId) {
        return query(t -> assigneeId.equals(t.getAssigneeId()));
    }

    @Override
    public List<Task> findByParentId(String parentId) {
        return query(t -> parentId.equals(t.getParentId()));
    }

    @Override
    public List<Task> findDependents(String taskId) {
        return query(t -> t.getDependencyIds().contains(taskId));
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }

    private List<Task> query(Predicate<Task> predicate) {
        return store.values().stream()
                .filter(predicate)
                .map(Task::copy)
                .sorted(Comparator.comparing(Task::getId))
                .toList();
    }
}
