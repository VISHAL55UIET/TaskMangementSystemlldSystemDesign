package com.TaskManageMentSystem.TaskManageMentSystem.enums;

public enum Priority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    Priority(int weight) {
        this.weight = weight;
    }

    /** Numeric weight used by sort strategies (higher = more urgent). */
    public int getWeight() {
        return weight;
    }
}
