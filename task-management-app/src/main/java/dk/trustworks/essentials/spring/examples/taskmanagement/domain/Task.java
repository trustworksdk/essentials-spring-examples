package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.components.eventsourced.aggregates.EventHandler;
import dk.trustworks.essentials.components.eventsourced.aggregates.stateful.modern.AggregateRoot;
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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Task extends AggregateRoot<TaskId, TaskEvent, Task> {
    private TaskStatus status;
    private String title;
    private AssigneeId assignee;
    private Priority priority;
    private String description;
    private Set<String> tags;
    private Set<CommentId> commentIds;

    public Task(TaskId aggregateId) {
        super(aggregateId);
    }

    public Task(CreateTask cmd) {
        super(cmd.taskId());
        var normalizedTitle = normalizeTitle(cmd.title());
        if (normalizedTitle == null || cmd.priority() == null || cmd.createdAt() == null) {
            return;
        }

        apply(new TaskCreated(cmd.taskId(),
                              normalizedTitle,
                              normalizeDescription(cmd.description()),
                              cmd.assignee(),
                              cmd.priority(),
                              cmd.createdAt()));
    }

    public void start(StartTask cmd) {
        if (status != TaskStatus.OPEN || cmd.startedAt() == null) {
            return;
        }
        apply(new TaskStarted(aggregateId(), cmd.startedAt()));
    }

    public void complete(CompleteTask cmd) {
        if (status != TaskStatus.IN_PROGRESS || cmd.completedAt() == null) {
            return;
        }
        apply(new TaskCompleted(aggregateId(), cmd.completedAt()));
    }

    public void reopen(ReopenTask cmd) {
        if (status != TaskStatus.DONE || cmd.reopenedAt() == null) {
            return;
        }
        apply(new TaskReopened(aggregateId(), cmd.reopenedAt()));
    }

    public void assign(AssignTask cmd) {
        if (status == TaskStatus.DONE || cmd.assignedAt() == null) {
            return;
        }

        if (cmd.assignee() == null || cmd.assignee().toString().trim().isEmpty()) {
            return;
        }

        if (cmd.assignee().equals(assignee)) {
            return;
        }

        apply(new TaskAssigned(aggregateId(), cmd.assignee(), cmd.assignedAt()));
    }

    public void unassign(UnassignTask cmd) {
        if (status == TaskStatus.DONE || cmd.unassignedAt() == null) {
            return;
        }
        if (assignee == null) {
            return;
        }
        apply(new TaskUnassigned(aggregateId(), cmd.unassignedAt()));
    }

    public void addComment(AddComment cmd) {
        if (status == TaskStatus.DONE || cmd.createdAt() == null || cmd.commentId() == null) {
            return;
        }

        var normalizedContent = normalizeComment(cmd.content());
        if (normalizedContent == null) {
            return;
        }

        if (commentIds.contains(cmd.commentId())) {
            return;
        }

        apply(new CommentAdded(aggregateId(), cmd.commentId(), normalizedContent, cmd.createdAt()));
    }

    public void tag(TagTask cmd) {
        if (cmd.taggedAt() == null) {
            return;
        }

        var normalized = TagNormalizer.normalize(cmd.tag()).orElse(null);
        if (normalized == null) {
            return;
        }

        if (tags.contains(normalized) || tags.size() >= 20) {
            return;
        }

        apply(new TaskTagged(aggregateId(), normalized, cmd.taggedAt()));
    }

    public void untag(UntagTask cmd) {
        if (cmd.untaggedAt() == null) {
            return;
        }

        var normalized = TagNormalizer.normalize(cmd.tag()).orElse(null);
        if (normalized == null || !tags.contains(normalized)) {
            return;
        }

        apply(new TaskUntagged(aggregateId(), normalized, cmd.untaggedAt()));
    }

    public void updateDescription(UpdateTaskDescription cmd) {
        if (cmd.updatedAt() == null) {
            return;
        }
        var normalized = normalizeDescription(cmd.description());
        if (Objects.equals(normalized, description)) {
            return;
        }
        apply(new TaskDescriptionUpdated(aggregateId(), normalized, cmd.updatedAt()));
    }

    @EventHandler
    private void on(TaskCreated event) {
        status = TaskStatus.OPEN;
        title = event.title();
        description = event.description();
        assignee = event.assignee();
        priority = event.priority();
        tags = new LinkedHashSet<>();
        commentIds = new LinkedHashSet<>();
    }

    @EventHandler
    private void on(TaskStarted event) {
        status = TaskStatus.IN_PROGRESS;
    }

    @EventHandler
    private void on(TaskCompleted event) {
        status = TaskStatus.DONE;
    }

    @EventHandler
    private void on(TaskReopened event) {
        status = TaskStatus.OPEN;
    }

    @EventHandler
    private void on(TaskAssigned event) {
        assignee = event.assignee();
    }

    @EventHandler
    private void on(TaskUnassigned event) {
        assignee = null;
    }

    @EventHandler
    private void on(CommentAdded event) {
        commentIds.add(event.commentId());
    }

    @EventHandler
    private void on(TaskTagged event) {
        tags.add(event.tag());
    }

    @EventHandler
    private void on(TaskUntagged event) {
        tags.remove(event.tag());
    }

    @EventHandler
    private void on(TaskDescriptionUpdated event) {
        description = event.description();
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        var normalized = title.trim();
        if (normalized.isBlank() || normalized.length() > 140) {
            return null;
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        var normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeComment(String content) {
        if (content == null) {
            return null;
        }
        var normalized = content.trim();
        if (normalized.isBlank() || normalized.length() > 2000) {
            return null;
        }
        return normalized;
    }
}
