package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AddComment;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AssignTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CompleteTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.ReopenTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.StartTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.TagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UntagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UnassignTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UpdateTaskDescription;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.CommentAdded;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskAssigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCompleted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskDescriptionUpdated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskEvent;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskReopened;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskStarted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskTagged;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUnassigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUntagged;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskAggregateSpecTest {
    private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-01T11:00:00Z");
    private static final Instant T2 = Instant.parse("2026-03-01T12:00:00Z");

    @Test
    void createTaskEmitsTaskCreated() {
        var taskId = TaskId.random();
        var emitted = Scenario.given(List.of())
                              .whenCreate(new CreateTask(taskId, "  Implement CQRS demo  ", "desc", AssigneeId.of("alice"), Priority.HIGH, T0));

        assertThat(emitted).containsExactly(new TaskCreated(taskId, "Implement CQRS demo", "desc", AssigneeId.of("alice"), Priority.HIGH, T0));
    }

    @Test
    void createDuplicateEmitsNoEvents() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .whenCreate(new CreateTask(taskId, "Task", null, null, Priority.MEDIUM, T1));

        assertThat(emitted).isEmpty();
    }

    @Test
    void startOnlyFromOpen() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .when(task -> task.start(new StartTask(taskId, T1)));

        assertThat(emitted).containsExactly(new TaskStarted(taskId, T1));
    }

    @Test
    void completeOnlyFromInProgress() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .when(task -> task.complete(new CompleteTask(taskId, T1)));

        assertThat(emitted).isEmpty();
    }

    @Test
    void reopenOnlyFromDone() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0),
                                         new TaskStarted(taskId, T1),
                                         new TaskCompleted(taskId, T2));

        var emitted = Scenario.given(history)
                              .when(task -> task.reopen(new ReopenTask(taskId, T2.plusSeconds(60))));

        assertThat(emitted).containsExactly(new TaskReopened(taskId, T2.plusSeconds(60)));
    }

    @Test
    void assignSameAssigneeIsNoOp() {
        var taskId = TaskId.random();
        var assignee = AssigneeId.of("alice");
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, assignee, Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .when(task -> task.assign(new AssignTask(taskId, assignee, T1)));

        assertThat(emitted).isEmpty();
    }

    @Test
    void assignInDoneIsNoOp() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0),
                                         new TaskStarted(taskId, T1),
                                         new TaskCompleted(taskId, T2));

        var emitted = Scenario.given(history)
                              .when(task -> task.assign(new AssignTask(taskId, AssigneeId.of("bob"), T2.plusSeconds(1))));

        assertThat(emitted).isEmpty();
    }

    @Test
    void addCommentDedupesByCommentId() {
        var taskId = TaskId.random();
        var commentId = CommentId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0),
                                         new CommentAdded(taskId, commentId, "hello", T1));

        var emitted = Scenario.given(history)
                              .when(task -> task.addComment(new AddComment(taskId, commentId, "again", T2)));

        assertThat(emitted).isEmpty();
    }

    @Test
    void addCommentInDoneIsNoOp() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0),
                                         new TaskStarted(taskId, T1),
                                         new TaskCompleted(taskId, T2));

        var emitted = Scenario.given(history)
                              .when(task -> task.addComment(new AddComment(taskId, CommentId.random(), "new", T2.plusSeconds(1))));

        assertThat(emitted).isEmpty();
    }

    @Test
    void tagNormalizesAndIsIdempotent() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0));

        var first = Scenario.given(history)
                            .when(task -> task.tag(new TagTask(taskId, "  Urgent ", T1)));
        assertThat(first).containsExactly(new TaskTagged(taskId, "urgent", T1));

        var secondHistory = new ArrayList<>(history);
        secondHistory.addAll(first);
        var second = Scenario.given(secondHistory)
                             .when(task -> task.tag(new TagTask(taskId, "urgent", T2)));
        assertThat(second).isEmpty();
    }

    @Test
    void untagMissingIsNoOp() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .when(task -> task.untag(new UntagTask(taskId, "urgent", T1)));

        assertThat(emitted).isEmpty();
    }

    @Test
    void untagExistingEmitsTaskUntagged() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", null, null, Priority.MEDIUM, T0),
                                         new TaskTagged(taskId, "urgent", T1));

        var emitted = Scenario.given(history)
                              .when(task -> task.untag(new UntagTask(taskId, " urgent ", T2)));

        assertThat(emitted).containsExactly(new TaskUntagged(taskId, "urgent", T2));
    }

    @Test
    void updateDescriptionEmitsTaskDescriptionUpdated() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", "old", null, Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .when(task -> task.updateDescription(new UpdateTaskDescription(taskId, " new desc ", T1)));

        assertThat(emitted).containsExactly(new TaskDescriptionUpdated(taskId, "new desc", T1));
    }

    @Test
    void unassignEmitsTaskUnassignedWhenAssigned() {
        var taskId = TaskId.random();
        var history = List.<TaskEvent>of(new TaskCreated(taskId, "Task", "desc", AssigneeId.of("alice"), Priority.MEDIUM, T0));

        var emitted = Scenario.given(history)
                              .when(task -> task.unassign(new UnassignTask(taskId, T1)));

        assertThat(emitted).containsExactly(new TaskUnassigned(taskId, T1));
    }

    private record Scenario(List<TaskEvent> given) {
        static Scenario given(List<TaskEvent> history) {
            return new Scenario(history);
        }

        List<TaskEvent> whenCreate(CreateTask cmd) {
            if (!given.isEmpty()) {
                return List.of();
            }
            var task = new Task(cmd);
            return List.copyOf(task.getUncommittedChanges().events);
        }

        List<TaskEvent> when(TaskAction action) {
            var taskId = given.isEmpty() ? TaskId.random() : given.get(0).getTaskId();
            var aggregate = new Task(taskId).rehydrate(given.stream());
            action.apply(aggregate);
            return List.copyOf(aggregate.getUncommittedChanges().events);
        }
    }

    @FunctionalInterface
    private interface TaskAction {
        void apply(Task task);
    }
}
