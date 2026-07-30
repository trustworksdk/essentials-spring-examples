package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record TaskUntagged(TaskId taskId,
                           String tag,
                           Instant untaggedAt) implements TaskEvent {
    @Override
    public TaskId getTaskId() {
        return taskId;
    }
}
