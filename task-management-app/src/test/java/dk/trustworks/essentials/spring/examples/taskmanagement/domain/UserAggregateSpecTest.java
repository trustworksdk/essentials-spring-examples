package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.RegisterUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserEvent;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserRegistered;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserAggregateSpecTest {
    private static final Instant T0 = Instant.parse("2026-03-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-01T10:01:00Z");

    @Test
    void createUserEmitsCreated() {
        var userId = UserId.of("alice");

        var emitted = List.copyOf(new User(new CreateUser(userId, " Alice  ", T0)).getUncommittedChanges().events);

        assertThat(emitted).containsExactly(new UserCreated(userId, "Alice", T0));
    }

    @Test
    void createUserWithBlankNameEmitsNoEvents() {
        var emitted = List.copyOf(new User(new CreateUser(UserId.of("alice"), "   ", T0)).getUncommittedChanges().events);
        assertThat(emitted).isEmpty();
    }

    @Test
    void registerFromCreatedEmitsRegistered() {
        var userId = UserId.of("alice");
        var aggregate = new User(userId);
        var history = List.<UserEvent>of(new UserCreated(userId, "Alice", T0));
        aggregate.rehydrate(history.stream());

        aggregate.register(new RegisterUser(userId, "Alice@Example.com", "$2a$10$hash", T1));

        assertThat(List.copyOf(aggregate.getUncommittedChanges().events)).containsExactly(new UserRegistered(userId, "alice@example.com", "$2a$10$hash", T1));
    }

    @Test
    void registerTwiceSecondIsNoOp() {
        var userId = UserId.of("alice");
        var aggregate = new User(userId);
        var history = List.<UserEvent>of(new UserCreated(userId, "Alice", T0),
                                         new UserRegistered(userId, "alice@example.com", "$2a$10$hash", T1));
        aggregate.rehydrate(history.stream());

        aggregate.register(new RegisterUser(userId, "alice+2@example.com", "$2a$10$hash2", T1.plusSeconds(1)));

        assertThat(List.copyOf(aggregate.getUncommittedChanges().events)).isEmpty();
    }
}
