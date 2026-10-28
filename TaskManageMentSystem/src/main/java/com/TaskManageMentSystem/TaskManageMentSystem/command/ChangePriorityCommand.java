package com.TaskManageMentSystem.TaskManageMentSystem.command;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.service.TaskService;

public class ChangePriorityCommand implements TaskCommand {

    private final TaskService service;
    private final String actorId;
    private final String taskId;
    private final Priority newPriority;
    private Priority previousPriority;

    public ChangePriorityCommand(TaskService service, String actorId, String taskId, Priority newPriority) {
        this.service = service;
        this.actorId = actorId;
        this.taskId = taskId;
        this.newPriority = newPriority;
    }

    @Override
    public void execute() {
        previousPriority = service.getTask(taskId).getPriority();
        service.changePriority(actorId, taskId, newPriority);
    }

    @Override
    public void undo() {
        service.changePriority(actorId, taskId, previousPriority);
    }

    @Override
    public String description() {
        return "change priority of " + taskId + " to " + newPriority;
    }
}
