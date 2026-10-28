package com.TaskManageMentSystem.TaskManageMentSystem.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Invoker with bounded undo history and a redo stack (one instance per user session). */
public class CommandManager {

    private final Deque<TaskCommand> undoStack = new ArrayDeque<>();
    private final Deque<TaskCommand> redoStack = new ArrayDeque<>();
    private final int capacity;

    public CommandManager() {
        this(50);
    }

    public CommandManager(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void execute(TaskCommand command) {
        command.execute();
        undoStack.push(command);
        if (undoStack.size() > capacity) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public synchronized Optional<String> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        TaskCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return Optional.of(command.description());
    }

    public synchronized Optional<String> redo() {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        TaskCommand command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return Optional.of(command.description());
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
