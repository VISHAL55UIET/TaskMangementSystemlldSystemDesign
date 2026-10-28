package com.TaskManageMentSystem.TaskManageMentSystem.command;

/** Command pattern: every user action is an object that knows how to undo itself. */
public interface TaskCommand {

    void execute();

    void undo();

    String description();
}
