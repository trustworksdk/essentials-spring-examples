package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.AssigneeId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record AssignTask(TaskId taskId,
                         AssigneeId assignee,
                         Instant assignedAt) implements TaskCommand {
}
