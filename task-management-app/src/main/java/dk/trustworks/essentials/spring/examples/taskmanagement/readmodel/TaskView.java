package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskStatus;

import java.time.Instant;
import java.util.Set;

public record TaskView(String taskId,
                       String title,
                       String description,
                       Priority priority,
                       TaskStatus status,
                       String assignee,
                       Set<String> tags,
                       int commentsCount,
                       Instant createdAt,
                       Instant updatedAt,
                       Instant lastCommentAt) {
}
