package com.TaskManageMentSystem.TaskManageMentSystem.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe, human-readable ids per prefix: TASK-0001, USR-0001, CMT-0001 ... */
public final class IdGenerator {

    private static final ConcurrentMap<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();

    private IdGenerator() {
    }

    public static String next(String prefix) {
        long n = COUNTERS.computeIfAbsent(prefix, p -> new AtomicLong()).incrementAndGet();
        return String.format("%s-%04d", prefix, n);
    }
}
