package com.TaskManageMentSystem.TaskManageMentSystem.service;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.NotificationService;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background job that raises one OVERDUE event per overdue task.
 * {@link #runOnce()} does the work (easy to call from tests); {@link #start} runs it periodically.
 */
public class OverdueScheduler implements AutoCloseable {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final TaskService taskService;
    private final NotificationService notifications;
    private final Clock clock;
    private final Set<String> alreadyNotified = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService executor;

    public OverdueScheduler(TaskService taskService, NotificationService notifications, Clock clock) {
        this.taskService = taskService;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** @return how many new overdue notifications were sent in this run */
    public int runOnce() {
        var overdue = taskService.getOverdueTasks();
        // forget tasks that are no longer overdue, so they can alert again if they become overdue later
        alreadyNotified.retainAll(overdue.stream().map(Task::getId).toList());

        int sent = 0;
        for (Task task : overdue) {
            if (alreadyNotified.add(task.getId())) {
                Set<String> recipients = new LinkedHashSet<>();
                if (task.getAssigneeId() != null) {
                    recipients.add(task.getAssigneeId());
                }
                recipients.add(task.getCreatorId());
                notifications.publish(new TaskEvent(TaskEventType.OVERDUE, task, SYSTEM_ACTOR,
                        "Task is overdue: " + task.getTitle(), recipients, clock.instant()));
                sent++;
            }
        }
        return sent;
    }

    public synchronized void start(long period, TimeUnit unit) {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "overdue-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                runOnce();
            } catch (RuntimeException e) {
                System.err.println("Overdue check failed: " + e.getMessage());
            }
        }, 0, period, unit);
    }

    @Override
    public synchronized void close() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
