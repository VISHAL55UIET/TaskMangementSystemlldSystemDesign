package com.TaskManageMentSystem.TaskManageMentSystem.command;

import com.TaskManageMentSystem.TaskManageMentSystem.service.TaskService;

public class AssignTaskCommand implements TaskCommand {

    private final TaskService service;
    private final String actorId;
    private final String taskId;
    private final String newAssigneeId;
    private String previousAssigneeId;

    public AssignTaskCommand(TaskService service, String actorId, String taskId, String newAssigneeId) {
        this.service = service;
        this.actorId = actorId;
        this.taskId = taskId;
        this.newAssigneeId = newAssigneeId;
    }

    @Override
    public void execute() {
        previousAssigneeId = service.getTask(taskId).getAssigneeId();
        service.assignTask(actorId, taskId, newAssigneeId);
    }

    @Override
    public void undo() {
        service.restoreAssignee(actorId, taskId, previousAssigneeId);
    }

    @Override
    public String description() {
        return "assign " + taskId + " to " + newAssigneeId;
    }
}
