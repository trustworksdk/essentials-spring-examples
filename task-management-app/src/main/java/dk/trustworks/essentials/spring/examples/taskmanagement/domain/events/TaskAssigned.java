package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.AssigneeId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record TaskAssigned(TaskId taskId,
                           AssigneeId assignee,
                           Instant assignedAt) implements TaskEvent {
    @Override
    public TaskId getTaskId() {
        return taskId;
    }
}
