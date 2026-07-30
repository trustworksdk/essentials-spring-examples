package dk.trustworks.essentials.spring.examples.taskmanagement.domain.events;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;

import java.time.Instant;

public record UserRegistered(UserId userId,
                             String email,
                             String passwordHash,
                             Instant registeredAt) implements UserEvent {
}
