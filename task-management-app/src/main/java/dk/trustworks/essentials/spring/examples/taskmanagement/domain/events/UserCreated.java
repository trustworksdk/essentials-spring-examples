package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;

import java.time.Instant;

public record UserCreated(UserId userId,
                          String displayName,
                          Instant createdAt) implements UserEvent {
}
