package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.components.eventsourced.aggregates.EventHandler;
import dk.trustworks.essentials.components.eventsourced.aggregates.stateful.modern.AggregateRoot;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.RegisterUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserEvent;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserRegistered;

public class User extends AggregateRoot<UserId, UserEvent, User> {
    private UserStatus status;
    private String displayName;
    private String email;
    private String passwordHash;

    public User(UserId aggregateId) {
        super(aggregateId);
    }

    public User(CreateUser cmd) {
        super(cmd.userId());
        var normalizedDisplayName = normalizeDisplayName(cmd.displayName());
        if (normalizedDisplayName == null || cmd.createdAt() == null) {
            return;
        }

        apply(new UserCreated(cmd.userId(), normalizedDisplayName, cmd.createdAt()));
    }

    public void register(RegisterUser cmd) {
        if (status != UserStatus.CREATED || cmd.registeredAt() == null) {
            return;
        }

        var normalizedEmail = normalizeEmail(cmd.email());
        var normalizedPasswordHash = normalizePasswordHash(cmd.passwordHash());
        if (normalizedEmail == null || normalizedPasswordHash == null) {
            return;
        }

        apply(new UserRegistered(aggregateId(), normalizedEmail, normalizedPasswordHash, cmd.registeredAt()));
    }

    @EventHandler
    private void on(UserCreated event) {
        status = UserStatus.CREATED;
        displayName = event.displayName();
    }

    @EventHandler
    private void on(UserRegistered event) {
        status = UserStatus.REGISTERED;
        email = event.email();
        passwordHash = event.passwordHash();
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        var normalized = displayName.trim();
        if (normalized.isBlank() || normalized.length() > 120) {
            return null;
        }
        return normalized;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        var normalized = email.trim().toLowerCase();
        if (normalized.isBlank() || normalized.length() > 320 || !normalized.contains("@")) {
            return null;
        }
        return normalized;
    }

    private static String normalizePasswordHash(String passwordHash) {
        if (passwordHash == null) {
            return null;
        }
        var normalized = passwordHash.trim();
        if (normalized.isBlank() || normalized.length() > 255) {
            return null;
        }
        return normalized;
    }
}
