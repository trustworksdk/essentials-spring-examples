package dk.trustworks.essentials.spring.examples.taskmanagement.commands;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;

import java.time.Instant;

public record CreateUser(UserId userId,
                         String displayName,
                         Instant createdAt) implements UserCommand {
}
