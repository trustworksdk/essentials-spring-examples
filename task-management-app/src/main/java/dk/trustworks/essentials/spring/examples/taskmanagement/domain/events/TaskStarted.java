package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record TaskStarted(TaskId taskId,
                          Instant startedAt) implements TaskEvent {
    @Override
    public TaskId getTaskId() {
        return taskId;
    }
}
