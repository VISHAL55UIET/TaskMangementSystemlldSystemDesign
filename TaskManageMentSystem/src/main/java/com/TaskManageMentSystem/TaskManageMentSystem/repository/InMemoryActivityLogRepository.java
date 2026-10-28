package com.TaskManageMentSystem.TaskManageMentSystem.repository;

import com.TaskManageMentSystem.TaskManageMentSystem.model.ActivityLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryActivityLogRepository implements ActivityLogRepository {

    private final List<ActivityLog> entries = new CopyOnWriteArrayList<>();

    @Override
    public void save(ActivityLog entry) {
        entries.add(entry);
    }

    @Override
    public List<ActivityLog> findByTaskId(String taskId) {
        return entries.stream().filter(e -> e.taskId().equals(taskId)).toList();
    }

    @Override
    public List<ActivityLog> findAll() {
        return List.copyOf(entries);
    }
}
