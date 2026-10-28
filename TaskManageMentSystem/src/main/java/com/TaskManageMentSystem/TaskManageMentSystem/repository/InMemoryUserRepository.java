package com.TaskManageMentSystem.TaskManageMentSystem.repository;

import com.TaskManageMentSystem.TaskManageMentSystem.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentMap<String, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String wanted = email.toLowerCase();
        return store.values().stream().filter(u -> u.getEmail().equals(wanted)).findFirst();
    }

    @Override
    public List<User> findAll() {
        return store.values().stream().sorted(Comparator.comparing(User::getId)).toList();
    }
}
