package dk.trustworks.essentials.spring.examples.taskmanagement.projection;

import dk.trustworks.essentials.components.eventsourced.aggregates.EventHandler;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskStatus;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.CommentAdded;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskAssigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCompleted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskReopened;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskStarted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskTagged;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUnassigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUntagged;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class TaskInMemoryProjection {
    private String taskId;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private String assignee;
    private final Set<String> tags = new LinkedHashSet<>();
    private int commentsCount;
    private Instant createdAt;
    private Instant updatedAt;

    @EventHandler
    private void on(TaskCreated event) {
        taskId = event.taskId().toString();
        title = event.title();
        description = event.description();
        priority = event.priority();
        status = TaskStatus.OPEN;
        assignee = event.assignee() != null ? event.assignee().toString() : null;
        createdAt = event.createdAt();
        updatedAt = event.createdAt();
    }

    @EventHandler
    private void on(TaskStarted event) {
        status = TaskStatus.IN_PROGRESS;
        updatedAt = event.startedAt();
    }

    @EventHandler
    private void on(TaskCompleted event) {
        status = TaskStatus.DONE;
        updatedAt = event.completedAt();
    }

    @EventHandler
    private void on(TaskReopened event) {
        status = TaskStatus.OPEN;
        updatedAt = event.reopenedAt();
    }

    @EventHandler
    private void on(TaskAssigned event) {
        assignee = event.assignee().toString();
        updatedAt = event.assignedAt();
    }

    @EventHandler
    private void on(TaskUnassigned event) {
        assignee = null;
        updatedAt = event.unassignedAt();
    }

    @EventHandler
    private void on(CommentAdded event) {
        commentsCount++;
        updatedAt = event.createdAt();
    }

    @EventHandler
    private void on(TaskTagged event) {
        tags.add(event.tag());
        updatedAt = event.taggedAt();
    }

    @EventHandler
    private void on(TaskUntagged event) {
        tags.remove(event.tag());
        updatedAt = event.untaggedAt();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Priority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
