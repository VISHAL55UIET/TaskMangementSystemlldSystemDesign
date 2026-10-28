package com.TaskManageMentSystem.TaskManageMentSystem.notificatioin;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.UserRepository;

import java.util.concurrent.atomic.AtomicInteger;

/** Simulated e-mail channel: prints what a real SMTP/SES adapter would send. */
public class EmailNotifier implements TaskObserver {

    private final UserRepository userRepository;
    private final AtomicInteger sentCount = new AtomicInteger();

    public EmailNotifier(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onEvent(TaskEvent event) {
        for (String userId : event.recipientIds()) {
            userRepository.findById(userId).ifPresent(user -> {
                sentCount.incrementAndGet();
                System.out.printf("   [EMAIL] to=%s | [%s] %s - %s%n",
                        user.getEmail(), event.type(), event.task().getId(), event.message());
            });
        }
    }

    public int sentCount() {
        return sentCount.get();
    }
}
