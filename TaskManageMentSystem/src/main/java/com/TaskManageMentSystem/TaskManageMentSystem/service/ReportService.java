package com.TaskManageMentSystem.TaskManageMentSystem.service;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskStats;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Permission;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.UserNotFoundException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.TaskRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorization;
    private final Clock clock;

    public ReportService(TaskRepository taskRepository, UserRepository userRepository,
                         AuthorizationService authorization, Clock clock) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.authorization = authorization;
        this.clock = clock;
    }

    public TaskStats generate(String actorId) {
        authorization.require(
                userRepository.findById(actorId).orElseThrow(() -> new UserNotFoundException(actorId)),
                Permission.VIEW_REPORTS);

        List<Task> tasks = taskRepository.findAll();
        Instant now = clock.instant();

        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, 0L);
        }
        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            byPriority.put(p, 0L);
        }
        tasks.forEach(t -> {
            byStatus.merge(t.getStatus(), 1L, Long::sum);
            byPriority.merge(t.getPriority(), 1L, Long::sum);
        });

        long overdue = tasks.stream().filter(t -> t.isOverdue(now)).count();
        long done = byStatus.get(TaskStatus.DONE);
        long counted = tasks.size() - byStatus.get(TaskStatus.CANCELLED);
        double completionRate = counted == 0 ? 0.0 : (double) done / counted;

        Map<String, Long> openByAssignee = tasks.stream()
                .filter(t -> t.getAssigneeId() != null && !t.getStatus().isClosed())
                .collect(Collectors.groupingBy(Task::getAssigneeId, TreeMap::new, Collectors.counting()));

        return new TaskStats(tasks.size(), byStatus, byPriority, overdue, completionRate, openByAssignee);
    }
}
