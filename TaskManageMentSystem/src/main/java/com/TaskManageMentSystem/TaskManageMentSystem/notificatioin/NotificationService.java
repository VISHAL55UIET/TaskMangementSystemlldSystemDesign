package com.TaskManageMentSystem.TaskManageMentSystem.notificatioin;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Subject of the observer pattern (event bus).
 *
 * - Observers can subscribe to everything, or only to specific event types.
 * - Delivery goes through an Executor: pass a thread pool to make notifications async,
 *   or use the default direct executor for deterministic, synchronous delivery.
 * - A failing observer never breaks the business operation or the other observers.
 */
public class NotificationService {

    private final List<TaskObserver> globalObservers = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<TaskEventType, List<TaskObserver>> typedObservers = new ConcurrentHashMap<>();
    private final Executor executor;

    public NotificationService() {
        this(Runnable::run);
    }

    public NotificationService(Executor executor) {
        this.executor = executor;
    }

    public void subscribe(TaskObserver observer) {
        globalObservers.add(observer);
    }

    public void subscribe(TaskEventType type, TaskObserver observer) {
        typedObservers.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(observer);
    }

    public void unsubscribe(TaskObserver observer) {
        globalObservers.remove(observer);
        typedObservers.values().forEach(list -> list.remove(observer));
    }

    public void publish(TaskEvent event) {
        globalObservers.forEach(o -> dispatch(o, event));
        typedObservers.getOrDefault(event.type(), List.of()).forEach(o -> dispatch(o, event));
    }

    private void dispatch(TaskObserver observer, TaskEvent event) {
        executor.execute(() -> {
            try {
                observer.onEvent(event);
            } catch (RuntimeException e) {
                System.err.println("Observer " + observer.getClass().getSimpleName()
                        + " failed for " + event.type() + ": " + e.getMessage());
            }
        });
    }
}
