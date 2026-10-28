package com.TaskManageMentSystem.TaskManageMentSystem.util;

import com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException;

public final class Preconditions {

    private Preconditions() {
    }
    /** Returns the trimmed value, or throws if it is null/blank. */
    public static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " must not be blank");
        }
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new ValidationException(field + " must not be null");
        }
        return value;
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}
