package com.TaskManageMentSystem.TaskManageMentSystem.dto;

import com.TaskManageMentSystem.TaskManageMentSystem.util.Preconditions;

/** Zero-based page index and page size (1..100). */
public record PageRequest(int page, int size) {

    public PageRequest {
        Preconditions.require(page >= 0, "page must be >= 0");
        Preconditions.require(size >= 1 && size <= 100, "size must be between 1 and 100");
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }
}
