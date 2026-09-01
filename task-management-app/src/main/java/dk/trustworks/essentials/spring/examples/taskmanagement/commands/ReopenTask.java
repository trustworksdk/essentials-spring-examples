package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record ReopenTask(TaskId taskId,
                         Instant reopenedAt) implements TaskCommand {
}
