package com.TaskManageMentSystem.TaskManageMentSystem.service;

import com.TaskManageMentSystem.TaskManageMentSystem.dto.CreateTaskRequest;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.Page;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.PageRequest;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskEvent;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskFilter;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Permission;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Role;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.DependencyNotSatisfiedException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.OptimisticLockException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.TaskNotFoundException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.UserNotFoundException;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.ValidationException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Comment;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.NotificationService;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.TaskRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.UserRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.strategy.AssignmentStrategy;
import com.TaskManageMentSystem.TaskManageMentSystem.strategy.SortStrategy;
import com.TaskManageMentSystem.TaskManageMentSystem.util.IdGenerator;
import com.TaskManageMentSystem.TaskManageMentSystem.util.Preconditions;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Application service: orchestrates validation, authorization, persistence and events.
 *
 * Every write follows the same shape: load a fresh copy, apply the change, save with the
 * version check, retry if somebody else won the race, and only then publish the event.
 * Events are published after a successful save so observers never see something that was
 * not persisted.
 */
public class TaskService {

    private static final int MAX_RETRIES = 100;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorization;
    private final DependencyService dependencies;
    private final NotificationService notifications;
    private final Clock clock;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       AuthorizationService authorization, DependencyService dependencies,
                       NotificationService notifications, Clock clock) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.authorization = authorization;
        this.dependencies = dependencies;
        this.notifications = notifications;
        this.clock = clock;
    }

    // ================================================================== commands

    public Task createTask(String actorId, CreateTaskRequest request) {
        User actor = requireUser(actorId);
        authorization.require(actor, Permission.CREATE_TASK);
        String title = Preconditions.requireNonBlank(request.getTitle(), "title");

        Instant now = clock.instant();
        if (request.getDueDate() != null && request.getDueDate().isBefore(now)) {
            throw new ValidationException("Due date cannot be in the past");
        }
        if (request.getAssigneeId() != null) {
            requireAssignable(actor, request.getAssigneeId());
        }
        if (request.getParentId() != null) {
            Task parent = getTask(request.getParentId());
            if (parent.getStatus().isClosed()) {
                throw new ValidationException("Cannot add a subtask to a closed task: " + parent.getId());
            }
        }

        Task task = Task.builder()
                .id(IdGenerator.next("TASK"))
                .title(title)
                .description(request.getDescription())
                .priority(request.getPriority())
                .assigneeId(request.getAssigneeId())
                .creatorId(actor.getId())
                .parentId(request.getParentId())
                .dueDate(request.getDueDate())
                .tags(request.getTags())
                .createdAt(now)
                .build();

        Task saved = taskRepository.save(task);
        publish(TaskEventType.CREATED, saved, actor, "Task created: " + saved.getTitle());
        if (saved.getAssigneeId() != null) {
            publish(TaskEventType.ASSIGNED, saved, actor,
                    "Assigned to " + userName(saved.getAssigneeId()) + ": " + saved.getTitle());
        }
        return saved;
    }

    public Task assignTask(String actorId, String taskId, String assigneeId) {
        User actor = requireUser(actorId);
        authorization.require(actor, Permission.ASSIGN_TASK);
        requireAssignable(actor, assigneeId);

        Task saved = update(taskId, t -> {
            requireNotClosed(t);
            t.assignTo(assigneeId);
        });
        publish(TaskEventType.ASSIGNED, saved, actor,
                "Assigned to " + userName(assigneeId) + ": " + saved.getTitle());
        return saved;
    }

    /** Picks an assignee among active MEMBERs using the given strategy, then assigns. */
    public Task autoAssign(String actorId, String taskId, AssignmentStrategy strategy) {
        User actor = requireUser(actorId);
        authorization.require(actor, Permission.ASSIGN_TASK);

        List<User> candidates = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(u -> u.getRole() == Role.MEMBER)
                .toList();
        User chosen = strategy.select(getTask(taskId), candidates, getOpenTaskCounts());
        return assignTask(actorId, taskId, chosen.getId());
    }

    public Task changeStatus(String actorId, String taskId, TaskStatus next) {
        User actor = requireUser(actorId);
        TaskStatus[] previous = new TaskStatus[1];

        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            previous[0] = t.getStatus();
            if (next == TaskStatus.DONE) {
                ensureCanComplete(t);
            }
            t.changeStatus(next);
        });
        publish(TaskEventType.STATUS_CHANGED, saved, actor,
                saved.getId() + ": " + previous[0] + " -> " + next);
        return saved;
    }

    public Task changePriority(String actorId, String taskId, Priority next) {
        User actor = requireUser(actorId);
        Priority[] previous = new Priority[1];

        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            requireNotClosed(t);
            previous[0] = t.getPriority();
            t.changePriority(next);
        });
        publish(TaskEventType.PRIORITY_CHANGED, saved, actor,
                saved.getId() + ": priority " + previous[0] + " -> " + next);
        return saved;
    }

    /** Pass null to clear the due date. */
    public Task updateDueDate(String actorId, String taskId, Instant dueDate) {
        User actor = requireUser(actorId);
        if (dueDate != null && dueDate.isBefore(clock.instant())) {
            throw new ValidationException("Due date cannot be in the past");
        }
        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            requireNotClosed(t);
            t.changeDueDate(dueDate);
        });
        publish(TaskEventType.DUE_DATE_CHANGED, saved, actor,
                saved.getId() + ": due date set to " + (dueDate == null ? "none" : dueDate));
        return saved;
    }

    public Task addComment(String actorId, String taskId, String text) {
        User actor = requireUser(actorId);
        authorization.require(actor, Permission.COMMENT);
        String body = Preconditions.requireNonBlank(text, "comment");
        Comment comment = new Comment(IdGenerator.next("CMT"), actor.getId(), body, clock.instant());

        Task saved = update(taskId, t -> t.addComment(comment));
        publish(TaskEventType.COMMENT_ADDED, saved, actor,
                actor.getName() + " commented on " + saved.getId());
        return saved;
    }

    public Task addTags(String actorId, String taskId, String... tags) {
        User actor = requireUser(actorId);
        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            for (String tag : tags) {
                t.addTag(tag);
            }
        });
        publish(TaskEventType.TAGS_ADDED, saved, actor, saved.getId() + ": tags now " + saved.getTags());
        return saved;
    }

    /** "taskId cannot finish until dependsOnId is DONE". Rejects self-links and cycles. */
    public Task addDependency(String actorId, String taskId, String dependsOnId) {
        User actor = requireUser(actorId);
        getTask(dependsOnId); // must exist
        dependencies.requireNoCycle(taskId, dependsOnId);

        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            t.addDependency(dependsOnId);
        });
        publish(TaskEventType.DEPENDENCY_ADDED, saved, actor,
                saved.getId() + " now depends on " + dependsOnId);
        return saved;
    }

    public void deleteTask(String actorId, String taskId) {
        User actor = requireUser(actorId);
        authorization.require(actor, Permission.DELETE_TASK);
        Task task = getTask(taskId);

        List<String> dependents = taskRepository.findDependents(taskId).stream().map(Task::getId).toList();
        if (!dependents.isEmpty()) {
            throw new ValidationException("Cannot delete " + taskId + ", tasks depend on it: " + dependents);
        }
        if (!taskRepository.findByParentId(taskId).isEmpty()) {
            throw new ValidationException("Cannot delete " + taskId + ", it still has subtasks");
        }
        taskRepository.deleteById(taskId);
        publish(TaskEventType.DELETED, task, actor, "Task deleted: " + task.getTitle());
    }

    // ------------------------------------------------ used by undo (skip the state machine)

    public Task restoreStatus(String actorId, String taskId, TaskStatus status) {
        User actor = requireUser(actorId);
        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            t.forceStatus(status);
        });
        publish(TaskEventType.STATUS_CHANGED, saved, actor, saved.getId() + ": status restored to " + status);
        return saved;
    }

    /** Pass null to unassign. */
    public Task restoreAssignee(String actorId, String taskId, String assigneeId) {
        User actor = requireUser(actorId);
        Task saved = update(taskId, t -> {
            authorization.requireCanModify(actor, t);
            t.assignTo(assigneeId);
        });
        publish(TaskEventType.ASSIGNED, saved, actor, saved.getId() + ": assignee restored to "
                + (assigneeId == null ? "nobody" : userName(assigneeId)));
        return saved;
    }

    // ================================================================== queries

    public Task getTask(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    public List<Task> getSubtasks(String parentId) {
        return taskRepository.findByParentId(parentId);
    }

    public Page<Task> search(TaskFilter filter, SortStrategy sort, PageRequest pageRequest) {
        List<Task> matches = taskRepository.findAll().stream()
                .filter(filter.toPredicate(clock.instant()))
                .sorted(sort.comparator())
                .toList();
        return Page.of(matches, pageRequest);
    }

    public List<Task> getOverdueTasks() {
        Instant now = clock.instant();
        return taskRepository.findAll().stream().filter(t -> t.isOverdue(now)).toList();
    }

    /** Open (not DONE/CANCELLED) task count per assignee id. */
    public Map<String, Long> getOpenTaskCounts() {
        return taskRepository.findAll().stream()
                .filter(t -> t.getAssigneeId() != null && !t.getStatus().isClosed())
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.counting()));
    }

    // ================================================================== internals

    /**
     * Optimistic-locking write loop: fresh copy -> mutate -> save. If another writer saved
     * first we get an OptimisticLockException and simply redo the change on the newer state.
     * The mutation must be safe to run more than once (it only touches the copy it is given).
     */
    private Task update(String taskId, Consumer<Task> mutation) {
        for (int attempt = 1; ; attempt++) {
            Task task = getTask(taskId);
            mutation.accept(task);
            task.touch(clock.instant());
            try {
                return taskRepository.save(task);
            } catch (OptimisticLockException e) {
                if (attempt >= MAX_RETRIES) {
                    throw e;
                }
            }
        }
    }

    private void ensureCanComplete(Task task) {
        List<String> pending = dependencies.unfinishedDependencies(task);
        if (!pending.isEmpty()) {
            throw new DependencyNotSatisfiedException(task.getId(), pending);
        }
        List<String> openSubtasks = taskRepository.findByParentId(task.getId()).stream()
                .filter(s -> !s.getStatus().isClosed())
                .map(Task::getId)
                .toList();
        if (!openSubtasks.isEmpty()) {
            throw new ValidationException("Task " + task.getId() + " has open subtasks: " + openSubtasks);
        }
    }

    private void requireNotClosed(Task task) {
        if (task.getStatus().isClosed()) {
            throw new ValidationException("Task " + task.getId() + " is " + task.getStatus()
                    + "; reopen it before changing it");
        }
    }

    private void requireAssignable(User actor, String assigneeId) {
        User assignee = requireUser(assigneeId);
        if (!assignee.isActive()) {
            throw new ValidationException("Cannot assign to inactive user: " + assignee.getName());
        }
        if (!assignee.getId().equals(actor.getId())) {
            authorization.require(actor, Permission.ASSIGN_TASK);
        }
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private String userName(String userId) {
        return userRepository.findById(userId).map(User::getName).orElse(userId);
    }

    private void publish(TaskEventType type, Task task, User actor, String message) {
        Set<String> recipients = new LinkedHashSet<>();
        if (task.getAssigneeId() != null) {
            recipients.add(task.getAssigneeId());
        }
        recipients.add(task.getCreatorId());
        recipients.remove(actor.getId()); // never notify people about their own actions
        notifications.publish(new TaskEvent(type, task, actor.getId(), message, recipients, clock.instant()));
    }
}
