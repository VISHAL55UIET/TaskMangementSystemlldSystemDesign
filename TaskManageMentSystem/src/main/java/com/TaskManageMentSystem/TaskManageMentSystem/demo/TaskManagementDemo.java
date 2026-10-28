package com.TaskManageMentSystem.TaskManageMentSystem.demo;

import com.TaskManageMentSystem.TaskManageMentSystem.command.AssignTaskCommand;
import com.TaskManageMentSystem.TaskManageMentSystem.command.ChangePriorityCommand;
import com.TaskManageMentSystem.TaskManageMentSystem.command.CommandManager;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.CreateTaskRequest;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.Page;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.PageRequest;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskFilter;
import com.TaskManageMentSystem.TaskManageMentSystem.dto.TaskStats;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Priority;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.Role;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskEventType;
import com.TaskManageMentSystem.TaskManageMentSystem.enums.TaskStatus;
import com.TaskManageMentSystem.TaskManageMentSystem.exception.TaskManagementException;
import com.TaskManageMentSystem.TaskManageMentSystem.model.Task;
import com.TaskManageMentSystem.TaskManageMentSystem.model.User;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.ActivityLogObserver;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.EmailNotifier;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.InAppNotifier;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.NotificationService;
import com.TaskManageMentSystem.TaskManageMentSystem.notificatioin.SlackNotifier;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.ActivityLogRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.InMemoryActivityLogRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.InMemoryTaskRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.InMemoryUserRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.TaskRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.repository.UserRepository;
import com.TaskManageMentSystem.TaskManageMentSystem.service.AuthorizationService;
import com.TaskManageMentSystem.TaskManageMentSystem.service.DependencyService;
import com.TaskManageMentSystem.TaskManageMentSystem.service.OverdueScheduler;
import com.TaskManageMentSystem.TaskManageMentSystem.service.ReportService;
import com.TaskManageMentSystem.TaskManageMentSystem.service.TaskService;
import com.TaskManageMentSystem.TaskManageMentSystem.service.UserService;
import com.TaskManageMentSystem.TaskManageMentSystem.strategy.LeastLoadedAssignmentStrategy;
import com.TaskManageMentSystem.TaskManageMentSystem.strategy.PrioritySortStrategy;
import com.TaskManageMentSystem.TaskManageMentSystem.strategy.RoundRobinAssignmentStrategy;
import com.TaskManageMentSystem.TaskManageMentSystem.util.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end walkthrough of the Task Management System.
 * Run it with the green arrow next to main(); nothing else is required.
 */
public class TaskManagementDemo {

    private static UserRepository userRepo;

    public static void main(String[] args) throws Exception {

        // ---------------------------------------------------------------- wiring (composition root)
        MutableClock clock = new MutableClock(Instant.parse("2026-11-02T09:00:00Z"));
        userRepo = new InMemoryUserRepository();
        TaskRepository taskRepo = new InMemoryTaskRepository();
        ActivityLogRepository logRepo = new InMemoryActivityLogRepository();

        NotificationService notifications = new NotificationService();
        EmailNotifier email = new EmailNotifier(userRepo);
        InAppNotifier inApp = new InAppNotifier();
        notifications.subscribe(new ActivityLogObserver(logRepo));              // every event -> audit log
        notifications.subscribe(inApp);                                         // every event -> in-app inbox
        notifications.subscribe(TaskEventType.ASSIGNED, email);                 // e-mail only on assignment...
        notifications.subscribe(TaskEventType.OVERDUE, email);                  // ...and on overdue
        notifications.subscribe(new SlackNotifier("#critical-alerts"));         // slack: CRITICAL + overdue

        AuthorizationService authorization = new AuthorizationService();
        UserService userService = new UserService(userRepo, authorization);
        DependencyService dependencyService = new DependencyService(taskRepo);
        TaskService taskService = new TaskService(taskRepo, userRepo, authorization,
                dependencyService, notifications, clock);
        ReportService reportService = new ReportService(taskRepo, userRepo, authorization, clock);
        OverdueScheduler overdueScheduler = new OverdueScheduler(taskService, notifications, clock);

        // ---------------------------------------------------------------- 1. users & roles
        header("1. Users and roles");
        User asha = userService.registerUser("Asha", "asha@taskhub.io", Role.ADMIN);
        User rohan = userService.registerUser("Rohan", "rohan@taskhub.io", Role.MANAGER);
        User neha = userService.registerUser("Neha", "neha@taskhub.io", Role.MEMBER);
        User karan = userService.registerUser("Karan", "karan@taskhub.io", Role.MEMBER);
        User vikas = userService.registerUser("Vikas", "vikas@taskhub.io", Role.VIEWER);
        userRepo.findAll().forEach(u -> System.out.println("   " + u));
        expectFailure("duplicate e-mail", () -> userService.registerUser("Fake", "ASHA@taskhub.io", Role.MEMBER));

        // ---------------------------------------------------------------- 2. create tasks
        header("2. Creating tasks (Rohan = manager)");
        Task schema = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                .title("Design database schema").description("Tables, indexes, migrations")
                .priority(Priority.HIGH).assigneeId(neha.getId()).dueDate(in(clock, 3))
                .tags("backend", "DB").build());
        Task apis = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                .title("Build REST APIs").priority(Priority.HIGH).assigneeId(karan.getId())
                .dueDate(in(clock, 7)).tags("backend").build());
        Task docs = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                .title("Write API documentation").priority(Priority.MEDIUM).dueDate(in(clock, 10))
                .tags("docs").build());
        Task outage = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                .title("Fix payment gateway outage").description("Customers see 502 on checkout")
                .priority(Priority.CRITICAL).assigneeId(neha.getId()).dueDate(in(clock, 1))
                .tags("incident", "payments").build());
        Task authApi = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                .title("Implement auth endpoint").priority(Priority.MEDIUM).assigneeId(karan.getId())
                .parentId(apis.getId()).build());
        List.of(schema, apis, docs, outage, authApi).forEach(t -> System.out.println("   created " + t));

        // ---------------------------------------------------------------- 3. dependencies
        header("3. Dependencies (apis needs schema, docs needs apis)");
        taskService.addDependency(rohan.getId(), apis.getId(), schema.getId());
        taskService.addDependency(rohan.getId(), docs.getId(), apis.getId());
        expectFailure("cycle: schema -> docs", () -> taskService.addDependency(rohan.getId(), schema.getId(), docs.getId()));
        expectFailure("self dependency", () -> taskService.addDependency(rohan.getId(), docs.getId(), docs.getId()));
        System.out.println("   valid execution order: " + ids(dependencyService.executionOrder(taskRepo.findAll())));

        // ---------------------------------------------------------------- 4. lifecycle & business rules
        header("4. Status state machine and completion rules");
        expectFailure("TODO -> DONE directly", () -> taskService.changeStatus(neha.getId(), schema.getId(), TaskStatus.DONE));
        taskService.changeStatus(neha.getId(), schema.getId(), TaskStatus.IN_PROGRESS);
        taskService.changeStatus(karan.getId(), apis.getId(), TaskStatus.IN_PROGRESS);
        expectFailure("finish apis while schema is not done", () -> taskService.changeStatus(karan.getId(), apis.getId(), TaskStatus.DONE));
        taskService.changeStatus(neha.getId(), schema.getId(), TaskStatus.IN_REVIEW);
        taskService.changeStatus(neha.getId(), schema.getId(), TaskStatus.DONE);
        System.out.println("   schema is now " + taskService.getTask(schema.getId()).getStatus());
        expectFailure("finish apis while subtask is open", () -> taskService.changeStatus(karan.getId(), apis.getId(), TaskStatus.DONE));
        taskService.changeStatus(karan.getId(), authApi.getId(), TaskStatus.IN_PROGRESS);
        taskService.changeStatus(karan.getId(), authApi.getId(), TaskStatus.DONE);
        taskService.changeStatus(karan.getId(), apis.getId(), TaskStatus.DONE);
        System.out.println("   apis is now " + taskService.getTask(apis.getId()).getStatus());

        // ---------------------------------------------------------------- 5. authorization
        header("5. Role-based authorization");
        expectFailure("viewer creates a task", () -> taskService.createTask(vikas.getId(),
                CreateTaskRequest.builder().title("Should fail").build()));
        expectFailure("member assigns to someone else", () -> taskService.assignTask(neha.getId(), docs.getId(), karan.getId()));
        expectFailure("member edits a task that is not theirs", () -> taskService.changePriority(karan.getId(), outage.getId(), Priority.LOW));
        expectFailure("manager deletes (admin only)", () -> taskService.deleteTask(rohan.getId(), docs.getId()));
        expectFailure("member views reports", () -> reportService.generate(neha.getId()));

        // ---------------------------------------------------------------- 6. assignment strategies
        header("6. Auto-assignment strategies");
        taskService.autoAssign(rohan.getId(), docs.getId(), new LeastLoadedAssignmentStrategy());
        System.out.println("   least-loaded picked: " + userName(taskService.getTask(docs.getId()).getAssigneeId())
                + " (Neha has the open incident, Karan has nothing open)");
        RoundRobinAssignmentStrategy roundRobin = new RoundRobinAssignmentStrategy();
        for (String title : List.of("Set up CI pipeline", "Add logging", "Write unit tests")) {
            Task t = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                    .title(title).priority(Priority.LOW).build());
            taskService.autoAssign(rohan.getId(), t.getId(), roundRobin);
            System.out.println("   round-robin: '" + title + "' -> " + userName(taskService.getTask(t.getId()).getAssigneeId()));
        }

        // ---------------------------------------------------------------- 7. concurrency
        header("7. Concurrency: optimistic locking");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        User[] authors = {asha, rohan, neha, karan};
        for (User author : authors) {
            futures.add(pool.submit(() -> {
                for (int i = 1; i <= 5; i++) {
                    taskService.addComment(author.getId(), outage.getId(), author.getName() + " note #" + i);
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("   4 threads x 5 comments -> " + taskService.getTask(outage.getId()).getComments().size()
                + " comments saved (expected 20, none lost)");

        Task copyA = taskRepo.findById(outage.getId()).orElseThrow();
        Task copyB = taskRepo.findById(outage.getId()).orElseThrow();
        copyA.updateDescription("Edited by A");
        taskRepo.save(copyA);
        copyB.updateDescription("Edited by B (stale)");
        expectFailure("stale write (lost update prevented)", () -> taskRepo.save(copyB));

        // ---------------------------------------------------------------- 8. search
        header("8. Search: all open tasks, sorted by priority, 2 per page");
        TaskFilter filter = TaskFilter.builder().onlyOpen(true).build();
        PrioritySortStrategy byPriority = new PrioritySortStrategy();
        int pageNo = 0;
        Page<Task> page;
        do {
            page = taskService.search(filter, byPriority, PageRequest.of(pageNo++, 2));
            System.out.println("   page " + (page.pageNumber() + 1) + "/" + Math.max(page.totalPages(), 1) + ":");
            page.content().forEach(t -> System.out.println("      " + t));
        } while (page.hasNext());
        TaskFilter byTag = TaskFilter.builder().tag("backend").keyword("api").build();
        System.out.println("   tag=backend + keyword='api': "
                + ids(taskService.search(byTag, byPriority, PageRequest.of(0, 10)).content()));

        // ---------------------------------------------------------------- 9. undo / redo
        header("9. Command pattern: undo / redo (Rohan)");
        Task chore = taskService.createTask(rohan.getId(), CreateTaskRequest.builder()
                .title("Rotate API keys").priority(Priority.LOW).build());
        CommandManager history = new CommandManager();
        history.execute(new ChangePriorityCommand(taskService, rohan.getId(), chore.getId(), Priority.HIGH));
        history.execute(new AssignTaskCommand(taskService, rohan.getId(), chore.getId(), karan.getId()));
        printChore("after 2 commands", taskService.getTask(chore.getId()));
        history.undo().ifPresent(d -> System.out.println("   undo: " + d));
        history.undo().ifPresent(d -> System.out.println("   undo: " + d));
        printChore("after 2 undos", taskService.getTask(chore.getId()));
        history.redo().ifPresent(d -> System.out.println("   redo: " + d));
        printChore("after 1 redo", taskService.getTask(chore.getId()));

        // ---------------------------------------------------------------- 10. overdue scheduler
        header("10. Overdue detection (clock moved forward 2 days)");
        clock.advance(Duration.ofDays(2));
        System.out.println("   first run  -> " + overdueScheduler.runOnce() + " new overdue alert(s)");
        System.out.println("   second run -> " + overdueScheduler.runOnce() + " new overdue alert(s) (deduplicated)");

        // ---------------------------------------------------------------- 11. report
        header("11. Report (Rohan)");
        TaskStats stats = reportService.generate(rohan.getId());
        System.out.println("   total tasks     : " + stats.totalTasks());
        System.out.println("   by status       : " + stats.byStatus());
        System.out.println("   by priority     : " + stats.byPriority());
        System.out.println("   overdue         : " + stats.overdueCount());
        System.out.printf("   completion rate : %.0f%%%n", stats.completionRate() * 100);
        stats.openTasksByAssignee().forEach((id, n) -> System.out.println("   open for " + userName(id) + ": " + n));

        // ---------------------------------------------------------------- 12. delete rules + audit
        header("12. Delete rules, audit log, in-app inbox");
        expectFailure("delete task that others depend on", () -> taskService.deleteTask(asha.getId(), schema.getId()));
        taskService.deleteTask(asha.getId(), chore.getId());
        System.out.println("   Asha deleted " + chore.getId());
        System.out.println("   audit trail for " + outage.getId() + ": "
                + logRepo.findByTaskId(outage.getId()).size() + " entries");
        System.out.println("   unread in-app notifications -> Neha: " + inApp.unreadCount(neha.getId())
                + ", Karan: " + inApp.unreadCount(karan.getId()));
        System.out.println("   e-mails sent: " + email.sentCount());

        overdueScheduler.close();
        System.out.println("\nDemo finished.");
    }

    // ===================================================================== helpers

    private static Instant in(MutableClock clock, int days) {
        return clock.instant().plus(Duration.ofDays(days));
    }

    private static void header(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static String userName(String userId) {
        return userRepo.findById(userId).map(User::getName).orElse(String.valueOf(userId));
    }

    private static List<String> ids(List<Task> tasks) {
        return tasks.stream().map(Task::getId).toList();
    }

    private static void printChore(String label, Task t) {
        System.out.println("   " + label + ": priority=" + t.getPriority() + ", assignee="
                + (t.getAssigneeId() == null ? "nobody" : userName(t.getAssigneeId())));
    }

    /** Runs an action that is supposed to be rejected and prints the reason. */
    private static void expectFailure(String label, Runnable action) {
        try {
            action.run();
            System.out.println("   [UNEXPECTED SUCCESS] " + label);
        } catch (TaskManagementException e) {
            System.out.println("   [BLOCKED] " + label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
