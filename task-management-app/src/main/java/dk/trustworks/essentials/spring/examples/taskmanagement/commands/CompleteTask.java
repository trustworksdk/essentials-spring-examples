package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record CompleteTask(TaskId taskId,
                           Instant completedAt) implements TaskCommand {
}
