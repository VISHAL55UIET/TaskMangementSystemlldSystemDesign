package com.TaskManageMentSystem.TaskManageMentSystem.dto;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;

import java.util.Map;

public record TaskStats(int totalTasks,
                        Map<TaskStatus, Long> byStatus,
                        Map<Priority, Long> byPriority,
                        long overdueCount,
                        double completionRate,
                        Map<String, Long> openTasksByAssignee) {
}
