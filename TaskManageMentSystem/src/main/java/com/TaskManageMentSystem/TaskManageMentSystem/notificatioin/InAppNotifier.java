package com.TaskManageMentSystem.TaskManageMentSystem.notificatioin;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Notification;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Keeps an in-app inbox per user (what a bell-icon dropdown would read). */
public class InAppNotifier implements TaskObserver {

    private final ConcurrentMap<String, List<Notification>> inbox = new ConcurrentHashMap<>();

    @Override
    public void onEvent(TaskEvent event) {
        for (String userId : event.recipientIds()) {
            inbox.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>())
                    .add(new Notification(userId, event.task().getId(), event.type(),
                            event.message(), event.occurredAt()));
        }
    }

    public List<Notification> unread(String userId) {
        return List.copyOf(inbox.getOrDefault(userId, List.of()));
    }

    public int unreadCount(String userId) {
        return inbox.getOrDefault(userId, List.of()).size();
    }

    public void markAllRead(String userId) {
        inbox.remove(userId);
    }
}
