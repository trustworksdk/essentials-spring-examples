package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.CommentId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

import java.time.Instant;

public record CommentAdded(TaskId taskId,
                           CommentId commentId,
                           String content,
                           Instant createdAt) implements TaskEvent {
    @Override
    public TaskId getTaskId() {
        return taskId;
    }
}
