package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.components.foundation.types.RandomIdGenerator;
import dk.trustworks.essentials.types.CharSequenceType;

public class TaskId extends CharSequenceType<TaskId> {
    public TaskId(String value) {
        super(value);
    }

    public TaskId(CharSequence value) {
        super(value);
    }

    public static TaskId random() {
        return new TaskId(RandomIdGenerator.generate());
    }

    public static TaskId of(String value) {
        return new TaskId(value);
    }
}
