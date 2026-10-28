# Task Management System - LLD design notes

Plain Java 17+ (no framework needed). Run `demo/TaskManagementDemo.main()` to see every feature.

## Package map

| Package        | What lives here |
|----------------|-----------------|
| `enums`        | `TaskStatus` (state machine), `Priority`, `Role`, `Permission`, `TaskEventType` |
| `model`        | `Task` (aggregate, versioned), `User`, `Comment`, `ActivityLog`, `Notification` |
| `dto`          | `CreateTaskRequest`, `TaskFilter`, `PageRequest`/`Page`, `TaskEvent`, `TaskStats` |
| `exception`    | `TaskManagementException` + 9 specific errors |
| `repository`   | interfaces + in-memory impls (swap for JPA later, services do not change) |
| `strategy`     | `SortStrategy` (priority, due date, newest, composite), `AssignmentStrategy` (least-loaded, round-robin) |
| `command`      | `TaskCommand`, 3 concrete commands, `CommandManager` (undo/redo) |
| `notificatioin`| Observer pattern: `NotificationService` (event bus), email / slack / in-app / audit-log observers |
| `service`      | `TaskService`, `UserService`, `AuthorizationService`, `DependencyService`, `ReportService`, `OverdueScheduler` |
| `util`         | `IdGenerator`, `MutableClock`, `Preconditions` |

(`notificatioin` keeps your original spelling so the project still lines up. In IntelliJ:
right-click the package > Refactor > Rename, or Shift+F6.)

## Design patterns used (and where)

- **Observer** - `TaskService` publishes `TaskEvent`; email, Slack, in-app inbox and audit log are independent observers. Adding a channel = one new class, zero changes in services.
- **Strategy** - sorting and auto-assignment are pluggable (`SortStrategy`, `AssignmentStrategy`).
- **Command** - every undoable action is an object with `execute()` / `undo()`; `CommandManager` holds undo and redo stacks.
- **Builder** - `Task`, `CreateTaskRequest`, `TaskFilter`.
- **State machine** - allowed status transitions are defined once, inside `TaskStatus`.
- **Repository** - services depend on interfaces, not storage.
- **Composite** - `CompositeSortStrategy` chains strategies.

## Business rules enforced

1. Status can only follow `TaskStatus` transitions (TODO -> DONE directly is rejected).
2. A task cannot be DONE while a dependency is not DONE, or while it has open subtasks.
3. Dependencies must not form a cycle (DFS check); `executionOrder` returns a topological order (Kahn).
4. Role-based permissions + ownership: members can edit only tasks they own/created; assigning others needs `ASSIGN_TASK`; delete is admin-only.
5. Cannot delete a task that others depend on or that has subtasks.
6. Closed tasks cannot change priority / due date / assignee until reopened.
7. Due dates must be in the future; users must be active to be assigned.

## Concurrency model

- The repository stores and returns **copies**, so nobody mutates stored state by accident.
- `Task.version` + `InMemoryTaskRepository.save()` = **optimistic locking**: a stale save throws `OptimisticLockException`.
- `TaskService.update()` runs *load copy -> mutate -> save* in a retry loop, so concurrent writers never lose updates (demo: 4 threads x 5 comments = 20 comments).
- Events are published only **after** a successful save.
- `NotificationService` takes an `Executor`: pass a thread pool to make notifications async.

## Time handling

Services take a `java.time.Clock`. `MutableClock` lets the demo (and tests) jump forward in time to trigger overdue logic without `sleep`.

## How to explain it in an interview (30 seconds)

"Layered design: model + enums hold the domain rules, repositories hide storage, services orchestrate. Cross-cutting concerns like notifications and audit logging use the Observer pattern so services stay clean. Sorting and assignment are Strategies, undo/redo uses Commands. For concurrency I used copy-on-read repositories with optimistic locking and a retry loop, so there are no global locks. Storage, notification channels and strategies can all be swapped without touching the services."

## Easy extensions (good follow-up answers)

- Replace in-memory repositories with JPA/Mongo implementations.
- Wrap services in Spring `@RestController`s; map exceptions with `@ControllerAdvice`.
- Recurring tasks, task templates, per-user notification preferences.
- Async notifications: `new NotificationService(Executors.newFixedThreadPool(4))`.
- Unit tests: inject `MutableClock` and in-memory repos, no mocks needed.
