package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.AssigneeId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record TaskCreated(TaskId taskId,
                          String title,
                          String description,
                          AssigneeId assignee,
                          Priority priority,
                          Instant createdAt) implements TaskEvent {
    @Override
    public TaskId getTaskId() {
        return taskId;
    }
}
