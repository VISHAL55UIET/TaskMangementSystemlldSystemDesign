package com.TaskManageMentSystem.TaskManageMentSystem.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Task lifecycle as a small state machine. The allowed transitions live next to the
 * states, so the rule exists in exactly one place.
 *
 * TODO -> IN_PROGRESS -> IN_REVIEW -> DONE (DONE can be re-opened to IN_PROGRESS)
 *            BLOCKED and CANCELLED are reachable from most states.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    BLOCKED,
    DONE,
    CANCELLED;

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        TRANSITIONS.put(TODO, EnumSet.of(IN_PROGRESS, BLOCKED, CANCELLED));
        TRANSITIONS.put(IN_PROGRESS, EnumSet.of(TODO, IN_REVIEW, BLOCKED, DONE, CANCELLED));
        TRANSITIONS.put(IN_REVIEW, EnumSet.of(IN_PROGRESS, DONE, CANCELLED));
        TRANSITIONS.put(BLOCKED, EnumSet.of(TODO, IN_PROGRESS, CANCELLED));
        TRANSITIONS.put(DONE, EnumSet.of(IN_PROGRESS));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(TaskStatus.class));
    }

    public boolean canTransitionTo(TaskStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public Set<TaskStatus> allowedTransitions() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    /** DONE or CANCELLED: no more work is expected on the task. */
    public boolean isClosed() {
        return this == DONE || this == CANCELLED;
    }
}
