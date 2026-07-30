package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

import java.time.Instant;

public record TaskCommentView(String taskId,
                              String commentId,
                              String content,
                              Instant createdAt) {
}
