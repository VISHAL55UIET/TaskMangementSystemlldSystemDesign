package com.TaskManageMentSystem.TaskManageMentSystem.notificatioin;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Simulated Slack channel: escalates important changes on CRITICAL tasks, plus every overdue alert.
 * Chatty events (comments, tags) are ignored so the channel stays readable.
 */
public class SlackNotifier implements TaskObserver {

    private static final Set<TaskEventType> NOISY = EnumSet.of(
            TaskEventType.COMMENT_ADDED, TaskEventType.TAGS_ADDED, TaskEventType.DEPENDENCY_ADDED);

    private final String channel;

    public SlackNotifier(String channel) {
        this.channel = channel;
    }

    @Override
    public void onEvent(TaskEvent event) {
        boolean critical = event.task().getPriority() == Priority.CRITICAL && !NOISY.contains(event.type());
        boolean overdue = event.type() == TaskEventType.OVERDUE;
        if (critical || overdue) {
            System.out.printf("   [SLACK %s] %s %s - %s%n",
                    channel, event.type(), event.task().getId(), event.message());
        }
    }
}
