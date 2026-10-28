package com.TaskManageMentSystem.TaskManageMentSystem.notificatioin;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.model.ActivityLog;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.ActivityLogRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.util.IdGenerator;

/**
 * Writes an audit trail for every event. Because it is just another observer, the
 * services stay free of logging code.
 */
public class ActivityLogObserver implements TaskObserver {

    private final ActivityLogRepository repository;

    public ActivityLogObserver(ActivityLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onEvent(TaskEvent event) {
        repository.save(new ActivityLog(IdGenerator.next("LOG"), event.task().getId(),
                event.actorId(), event.type(), event.message(), event.occurredAt()));
    }
}
