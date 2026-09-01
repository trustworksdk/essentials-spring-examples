package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record UpdateTaskDescription(TaskId taskId,
                                    String description,
                                    Instant updatedAt) implements TaskCommand {
}
