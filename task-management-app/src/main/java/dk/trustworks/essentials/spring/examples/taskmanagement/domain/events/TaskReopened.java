package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record TaskReopened(TaskId taskId,
                           Instant reopenedAt) implements TaskEvent {
    @Override
    public TaskId getTaskId() {
        return taskId;
    }
}
