package com.TaskManageMentSystem.TaskManageMentSystem.strategy;

import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Chains several strategies: the first one decides, the next ones break ties. */
public class CompositeSortStrategy implements SortStrategy {

    private final List<SortStrategy> strategies;

    public CompositeSortStrategy(SortStrategy... strategies) {
        if (strategies.length == 0) {
            throw new IllegalArgumentException("At least one strategy is required");
        }
        this.strategies = List.of(strategies);
    }

    @Override
    public Comparator<Task> comparator() {
        return strategies.stream()
                .map(SortStrategy::comparator)
                .reduce(Comparator::thenComparing)
                .orElseThrow();
    }

    @Override
    public String name() {
        return strategies.stream().map(SortStrategy::name).collect(Collectors.joining(" > "));
    }
}
