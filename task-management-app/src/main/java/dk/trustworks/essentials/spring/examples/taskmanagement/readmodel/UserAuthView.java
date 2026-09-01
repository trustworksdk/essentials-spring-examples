package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

public record UserAuthView(String userId,
                           String passwordHash,
                           boolean registered) {
}
