package dk.trustworks.essentials.spring.examples.taskmanagement;

import dk.trustworks.essentials.reactive.command.CommandBus;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AddComment;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CompleteTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.RegisterUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.StartTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.TagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.AssigneeId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.CommentId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskStatus;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;
import dk.trustworks.essentials.spring.examples.taskmanagement.projection.TaskInMemoryProjectionService;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskReadModels;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserReadModels;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DirtiesContext
@SpringBootTest(classes = Application.class)
class TaskManagementFlowIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withPassword("test")
            .withUsername("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Autowired
    private CommandBus commandBus;

    @Autowired
    private TaskReadModels taskReadModels;

    @Autowired
    private TaskInMemoryProjectionService taskInMemoryProjectionService;

    @Autowired
    private UserReadModels userReadModels;

    @Test
    void createStartCommentTagCompleteUpdatesPersistentReadModelsAndInMemoryProjection() {
        var taskId = TaskId.random();
        var t0 = Instant.parse("2026-03-01T10:00:00Z");
        var t1 = Instant.parse("2026-03-01T10:01:00Z");
        var t2 = Instant.parse("2026-03-01T10:02:00Z");
        var t3 = Instant.parse("2026-03-01T10:03:00Z");
        var t4 = Instant.parse("2026-03-01T10:04:00Z");
        var userId = UserId.of("alice");

        commandBus.send(new CreateUser(userId, "Alice", t0.minusSeconds(30)));
        commandBus.send(new RegisterUser(userId, "alice@example.com", "$2a$10$hash", t0.minusSeconds(10)));
        commandBus.send(new CreateTask(taskId,
                                       "Implement Task Management",
                                       "Spec-driven app",
                                       AssigneeId.of(userId.toString()),
                                       Priority.HIGH,
                                       t0));
        commandBus.send(new StartTask(taskId, t1));
        commandBus.send(new AddComment(taskId, CommentId.random(), "Looks good", t2));
        commandBus.send(new TagTask(taskId, "Urgent", t3));
        commandBus.send(new CompleteTask(taskId, t4));

        Awaitility.await()
                  .atMost(Duration.ofSeconds(4))
                  .untilAsserted(() -> {
                      var taskView = taskReadModels.findViewByTaskId(taskId.toString()).orElseThrow();
                      assertThat(taskView.status()).isEqualTo(TaskStatus.DONE);
                      assertThat(taskView.commentsCount()).isEqualTo(1);
                      assertThat(taskView.tags()).containsExactly("urgent");
                      assertThat(taskView.assignee()).isEqualTo(userId.toString());

                      var dashboard = taskReadModels.dashboardRows();
                      assertThat(dashboard).isNotEmpty();
                      assertThat(dashboard.get(0).taskId()).isEqualTo(taskId.toString());
                      assertThat(dashboard.get(0).status()).isEqualTo(TaskStatus.DONE);
                      assertThat(dashboard.get(0).assignee()).isEqualTo(userId.toString());

                      var userView = userReadModels.findByUserId(userId.toString()).orElseThrow();
                      assertThat(userView.registered()).isTrue();
                      assertThat(userView.email()).isEqualTo("alice@example.com");

                      var userTasks = taskReadModels.dashboardRowsForAssignee(userId.toString());
                      assertThat(userTasks).hasSize(1);
                      assertThat(userTasks.get(0).taskId()).isEqualTo(taskId.toString());

                      var inMemoryProjection = taskInMemoryProjectionService.project(taskId).orElseThrow();
                      assertThat(inMemoryProjection.getStatus()).isEqualTo(TaskStatus.DONE);
                      assertThat(inMemoryProjection.getCommentsCount()).isEqualTo(1);
                      assertThat(inMemoryProjection.getTags()).containsExactly("urgent");
                  });
    }
}
