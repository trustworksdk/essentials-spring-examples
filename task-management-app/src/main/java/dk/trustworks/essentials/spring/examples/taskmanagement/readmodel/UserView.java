package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

import java.time.Instant;

public record UserView(String userId,
                       String displayName,
                       String email,
                       boolean registered,
                       Instant createdAt,
                       Instant updatedAt) {
}
