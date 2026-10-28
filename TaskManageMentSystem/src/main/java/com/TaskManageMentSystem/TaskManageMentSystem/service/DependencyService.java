package com.TaskManageMentSystem.TaskManageMentSystem.service;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.CyclicDependencyException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.TaskRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph logic for task dependencies (task -> tasks it needs first).
 * Kept separate from TaskService so it can be tested and reasoned about on its own.
 */
public class DependencyService {

    private final TaskRepository taskRepository;

    public DependencyService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Would "taskId depends on dependsOnId" create a cycle? True when taskId is already
     * reachable from dependsOnId by following dependency edges (iterative DFS).
     */
    public boolean wouldCreateCycle(String taskId, String dependsOnId) {
        Set<String> visited = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(dependsOnId);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (current.equals(taskId)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            taskRepository.findById(current).ifPresent(t -> t.getDependencyIds().forEach(stack::push));
        }
        return false;
    }

    public void requireNoCycle(String taskId, String dependsOnId) {
        if (wouldCreateCycle(taskId, dependsOnId)) {
            throw new CyclicDependencyException(taskId, dependsOnId);
        }
    }

    /** Ids of dependencies that are not DONE yet (deleted dependencies are ignored). */
    public List<String> unfinishedDependencies(Task task) {
        List<String> pending = new ArrayList<>();
        for (String depId : task.getDependencyIds()) {
            taskRepository.findById(depId)
                    .filter(dep -> dep.getStatus() != TaskStatus.DONE)
                    .ifPresent(dep -> pending.add(dep.getId()));
        }
        return pending;
    }

    /**
     * Topological order (Kahn's algorithm): a valid sequence to execute the given tasks so
     * that every task comes after the tasks it depends on. Ties are broken by id.
     */
    public List<Task> executionOrder(Collection<Task> tasks) {
        Map<String, Task> byId = new HashMap<>();
        tasks.forEach(t -> byId.put(t.getId(), t));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (Task t : tasks) {
            inDegree.putIfAbsent(t.getId(), 0);
            for (String dep : t.getDependencyIds()) {
                if (byId.containsKey(dep)) {
                    inDegree.merge(t.getId(), 1, Integer::sum);
                    dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(t.getId());
                }
            }
        }

        java.util.PriorityQueue<String> ready = new java.util.PriorityQueue<>(Comparator.naturalOrder());
        inDegree.forEach((id, degree) -> {
            if (degree == 0) {
                ready.add(id);
            }
        });

        List<Task> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.poll();
            ordered.add(byId.get(id));
            for (String next : dependents.getOrDefault(id, List.of())) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(next);
                }
            }
        }
        if (ordered.size() != byId.size()) {
            throw new IllegalStateException("Dependency cycle detected among tasks");
        }
        return ordered;
    }
}
