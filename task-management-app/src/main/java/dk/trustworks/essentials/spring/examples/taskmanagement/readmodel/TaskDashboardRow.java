package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskStatus;

import java.time.Instant;

public record TaskDashboardRow(String taskId,
                               String title,
                               TaskStatus status,
                               Priority priority,
                               String assignee,
                               Instant updatedAt) {
}
