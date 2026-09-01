package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;

import java.time.Instant;

public record RegisterUser(UserId userId,
                           String email,
                           String passwordHash,
                           Instant registeredAt) implements UserCommand {
}
