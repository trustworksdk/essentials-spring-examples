package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record TagTask(TaskId taskId,
                      String tag,
                      Instant taggedAt) implements TaskCommand {
}
