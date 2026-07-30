package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.types.CharSequenceType;

public class AssigneeId extends CharSequenceType<AssigneeId> {
    public AssigneeId(String value) {
        super(value);
    }

    public AssigneeId(CharSequence value) {
        super(value);
    }

    public static AssigneeId of(String value) {
        return new AssigneeId(value);
    }
}
