package dk.trustworks.essentials.spring.examples.taskmanagement.api;

import dk.trustworks.essentials.reactive.command.CommandBus;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.RegisterUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskDashboardRow;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskReadModels;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserReadModels;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserManagementApi {
    private final CommandBus commandBus;
    private final UserReadModels userReadModels;
    private final TaskReadModels taskReadModels;
    private final PasswordEncoder passwordEncoder;

    public UserManagementApi(CommandBus commandBus,
                             UserReadModels userReadModels,
                             TaskReadModels taskReadModels,
                             PasswordEncoder passwordEncoder) {
        this.commandBus = commandBus;
        this.userReadModels = userReadModels;
        this.taskReadModels = taskReadModels;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public UserIdResponse create(@RequestBody CreateUserRequest request) {
        var userId = request.userId == null || request.userId.isBlank() ? UserId.random() : UserId.of(request.userId);
        commandBus.send(new CreateUser(userId, request.displayName, Instant.now()));
        return new UserIdResponse(userId.toString());
    }

    @PostMapping("/{userId}/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void register(@PathVariable String userId,
                         @RequestBody RegisterUserRequest request) {
        commandBus.sendAndDontWait(new RegisterUser(UserId.of(userId),
                                                    request.email,
                                                    passwordEncoder.encode(request.password),
                                                    Instant.now()));
    }

    @GetMapping("/{userId}")
    public UserView user(@PathVariable String userId) {
        return userReadModels.findByUserId(userId)
                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping("/{userId}/tasks")
    public List<TaskDashboardRow> tasksForUser(@PathVariable String userId) {
        if (!userReadModels.exists(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return taskReadModels.dashboardRowsForAssignee(userId);
    }

    public static final class CreateUserRequest {
        public String userId;
        @NotBlank
        public String displayName;
    }

    public static final class RegisterUserRequest {
        @Email
        @NotBlank
        public String email;
        @NotBlank
        @Size(min = 8, max = 128)
        public String password;
    }

    public record UserIdResponse(String userId) {
    }
}
