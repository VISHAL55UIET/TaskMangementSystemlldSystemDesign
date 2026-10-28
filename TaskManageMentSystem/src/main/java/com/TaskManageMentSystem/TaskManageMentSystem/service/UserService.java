package com.TaskManageMentSystem.TaskManageMentSystem.service;

import com.TaskManageMentSystem.TaskManageMentSystem.enums.Permission;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Role;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.UserNotFoundException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.UserRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.util.IdGenerator;

import java.util.List;

public class UserService {

    private final UserRepository userRepository;
    private final AuthorizationService authorization;

    public UserService(UserRepository userRepository, AuthorizationService authorization) {
        this.userRepository = userRepository;
        this.authorization = authorization;
    }

    /** Registration is open (sign-up); e-mail must be unique. */
    public synchronized User registerUser(String name, String email, Role role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("E-mail already registered: " + email);
        }
        return userRepository.save(new User(IdGenerator.next("USR"), name, email, role));
    }

    public User getUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    public User deactivateUser(String actorId, String userId) {
        authorization.require(getUser(actorId), Permission.MANAGE_USERS);
        User target = getUser(userId);
        target.setActive(false);
        return userRepository.save(target);
    }

    public User changeRole(String actorId, String userId, Role newRole) {
        authorization.require(getUser(actorId), Permission.MANAGE_USERS);
        User target = getUser(userId);
        target.setRole(newRole);
        return userRepository.save(target);
    }

    public List<User> findActiveByRole(Role role) {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() == role)
                .toList();
    }
}
