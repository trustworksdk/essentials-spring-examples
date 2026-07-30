package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.AssigneeId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record CreateTask(TaskId taskId,
                         String title,
                         String description,
                         AssigneeId assignee,
                         Priority priority,
                         Instant createdAt) implements TaskCommand {
}
