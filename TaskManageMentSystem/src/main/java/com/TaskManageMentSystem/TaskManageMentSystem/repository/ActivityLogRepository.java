package com.TaskManageMentSystem.TaskManageMentSystem.repository;

import com.TaskManageMentSystem.TaskManageMentSystem.model.ActivityLog;

import java.util.List;

public interface ActivityLogRepository {

    void save(ActivityLog entry);

    List<ActivityLog> findByTaskId(String taskId);

    List<ActivityLog> findAll();
}
