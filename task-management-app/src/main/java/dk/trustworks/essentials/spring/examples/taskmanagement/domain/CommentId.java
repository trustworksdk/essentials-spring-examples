package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.components.foundation.types.RandomIdGenerator;
import dk.trustworks.essentials.types.CharSequenceType;

public class CommentId extends CharSequenceType<CommentId> {
    public CommentId(String value) {
        super(value);
    }

    public CommentId(CharSequence value) {
        super(value);
    }

    public static CommentId random() {
        return new CommentId(RandomIdGenerator.generate());
    }

    public static CommentId of(String value) {
        return new CommentId(value);
    }
}
