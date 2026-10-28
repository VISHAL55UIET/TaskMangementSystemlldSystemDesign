package com.TaskManageMentSystem.TaskManageMentSystem.command;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.service.TaskService;

public class ChangeStatusCommand implements TaskCommand {

    private final TaskService service;
    private final String actorId;
    private final String taskId;
    private final TaskStatus newStatus;
    private TaskStatus previousStatus;

    public ChangeStatusCommand(TaskService service, String actorId, String taskId, TaskStatus newStatus) {
        this.service = service;
        this.actorId = actorId;
        this.taskId = taskId;
        this.newStatus = newStatus;
    }

    @Override
    public void execute() {
        previousStatus = service.getTask(taskId).getStatus();
        service.changeStatus(actorId, taskId, newStatus);
    }

    @Override
    public void undo() {
        service.restoreStatus(actorId, taskId, previousStatus);
    }

    @Override
    public String description() {
        return "change status of " + taskId + " to " + newStatus;
    }
}
