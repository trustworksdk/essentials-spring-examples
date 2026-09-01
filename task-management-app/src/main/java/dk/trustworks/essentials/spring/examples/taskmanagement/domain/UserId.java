package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.components.foundation.types.RandomIdGenerator;
import dk.trustworks.essentials.types.CharSequenceType;

public class UserId extends CharSequenceType<UserId> {
    public UserId(String value) {
        super(value);
    }

    public UserId(CharSequence value) {
        super(value);
    }

    public static UserId random() {
        return new UserId(RandomIdGenerator.generate());
    }

    public static UserId of(String value) {
        return new UserId(value);
    }
}
