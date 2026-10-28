package com.TaskManageMentSystem.TaskManageMentSystem.dto;

import java.util.List;

public record Page<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {

    public static <T> Page<T> of(List<T> all, PageRequest request) {
        int from = (int) Math.min((long) request.page() * request.size(), all.size());
        int to = Math.min(from + request.size(), all.size());
        return new Page<>(List.copyOf(all.subList(from, to)), request.page(), request.size(), all.size());
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    public boolean hasNext() {
        return pageNumber + 1 < totalPages();
    }
}
