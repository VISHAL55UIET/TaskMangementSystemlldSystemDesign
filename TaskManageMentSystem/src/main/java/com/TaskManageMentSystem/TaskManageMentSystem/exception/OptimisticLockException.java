package com.TaskManageMentSystem.TaskManageMentSystem.exception;

/** Thrown when a save is attempted with a stale version of an entity (lost-update protection). */
public class OptimisticLockException extends TaskManagementException {

    public OptimisticLockException(String taskId, long expectedVersion, long actualVersion) {
        super(String.format("Task %s was modified concurrently (you had version %d, current is %d)",
                taskId, expectedVersion, actualVersion));
    }
}
