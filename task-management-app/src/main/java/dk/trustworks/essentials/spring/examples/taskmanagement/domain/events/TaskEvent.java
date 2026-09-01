package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;

public interface TaskEvent {
    TaskId getTaskId();
}
