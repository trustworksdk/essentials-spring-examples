package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.CommentId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record AddComment(TaskId taskId,
                         CommentId commentId,
                         String content,
                         Instant createdAt) implements TaskCommand {
}
