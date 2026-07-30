package dk.trustworks.essentials.spring.examples.taskmanagement.api;

import dk.trustworks.essentials.reactive.command.CommandBus;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AddComment;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AssignTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CompleteTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.ReopenTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.StartTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.TagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UntagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UnassignTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UpdateTaskDescription;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.AssigneeId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.CommentId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;
import dk.trustworks.essentials.spring.examples.taskmanagement.projection.TaskInMemoryProjection;
import dk.trustworks.essentials.spring.examples.taskmanagement.projection.TaskInMemoryProjectionService;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskDashboardRow;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskCommentView;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskReadModels;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskView;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserReadModels;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/tasks")
public class TaskManagementApi {
    private static final Logger log = LoggerFactory.getLogger(TaskManagementApi.class);
    private final CommandBus commandBus;
    private final TaskReadModels taskReadModels;
    private final TaskInMemoryProjectionService taskInMemoryProjectionService;
    private final UserReadModels userReadModels;

    public TaskManagementApi(CommandBus commandBus,
                             TaskReadModels taskReadModels,
                             TaskInMemoryProjectionService taskInMemoryProjectionService,
                             UserReadModels userReadModels) {
        this.commandBus = commandBus;
        this.taskReadModels = taskReadModels;
        this.taskInMemoryProjectionService = taskInMemoryProjectionService;
        this.userReadModels = userReadModels;
    }

    @PostMapping
    public TaskIdResponse create(@RequestBody CreateTaskRequest request) {
        var taskId = request.taskId == null || request.taskId.isBlank() ? TaskId.random() : TaskId.of(request.taskId);
        var assignee = toAssigneeIdOrNull(request.assignee);
        var now = Instant.now();
        commandBus.send(new CreateTask(taskId,
                                       request.title,
                                       request.description,
                                       assignee,
                                       request.priority,
                                       now));
        log.info("CreateTask accepted taskId='{}' assignee='{}'", taskId, assignee);
        return new TaskIdResponse(taskId.toString());
    }

    @PostMapping("/{taskId}/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void start(@PathVariable String taskId) {
        commandBus.sendAndDontWait(new StartTask(TaskId.of(taskId), Instant.now()));
    }

    @PostMapping("/{taskId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void complete(@PathVariable String taskId) {
        commandBus.sendAndDontWait(new CompleteTask(TaskId.of(taskId), Instant.now()));
    }

    @PostMapping("/{taskId}/reopen")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reopen(@PathVariable String taskId) {
        commandBus.sendAndDontWait(new ReopenTask(TaskId.of(taskId), Instant.now()));
    }

    @PostMapping("/{taskId}/assign")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void assign(@PathVariable String taskId,
                       @RequestBody AssignTaskRequest request) {
        commandBus.sendAndDontWait(new AssignTask(TaskId.of(taskId), toAssigneeIdOrNull(request.assignee), Instant.now()));
    }

    @PostMapping("/{taskId}/unassign")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void unassign(@PathVariable String taskId) {
        commandBus.sendAndDontWait(new UnassignTask(TaskId.of(taskId), Instant.now()));
    }

    @PostMapping("/{taskId}/comments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addComment(@PathVariable String taskId,
                           @RequestBody AddCommentRequest request) {
        var commentId = request.commentId == null || request.commentId.isBlank() ? CommentId.random() : CommentId.of(request.commentId);
        commandBus.sendAndDontWait(new AddComment(TaskId.of(taskId), commentId, request.content, Instant.now()));
        log.info("AddComment accepted taskId='{}' commentId='{}'", taskId, commentId);
    }

    @PostMapping("/{taskId}/tags")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void tag(@PathVariable String taskId,
                    @RequestBody TagTaskRequest request) {
        commandBus.sendAndDontWait(new TagTask(TaskId.of(taskId), request.tag, Instant.now()));
    }

    @PostMapping("/{taskId}/untags")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void untag(@PathVariable String taskId,
                      @RequestBody TagTaskRequest request) {
        commandBus.sendAndDontWait(new UntagTask(TaskId.of(taskId), request.tag, Instant.now()));
    }

    @GetMapping("/{taskId}")
    public TaskView getTask(@PathVariable String taskId) {
        return taskReadModels.findViewByTaskId(taskId)
                             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @GetMapping("/dashboard")
    public List<TaskDashboardRow> dashboard() {
        return taskReadModels.dashboardRows();
    }

    @GetMapping("/{taskId}/in-memory-projection")
    public TaskInMemoryProjection inMemoryProjection(@PathVariable String taskId) {
        return taskInMemoryProjectionService.project(TaskId.of(taskId))
                                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @GetMapping("/{taskId}/comments")
    public List<TaskCommentView> comments(@PathVariable String taskId) {
        if (taskReadModels.findViewByTaskId(taskId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        return taskReadModels.commentsForTask(taskId);
    }

    @PostMapping("/{taskId}/description")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateDescription(@PathVariable String taskId, @RequestBody UpdateTaskDescriptionRequest request) {
        commandBus.sendAndDontWait(new UpdateTaskDescription(TaskId.of(taskId), request.description, Instant.now()));
        log.info("UpdateTaskDescription accepted taskId='{}'", taskId);
    }

    public static final class CreateTaskRequest {
        public String taskId;
        @NotBlank
        public String title;
        public String description;
        public String assignee;
        @NotNull
        public Priority priority;
    }

    public static final class AssignTaskRequest {
        @NotBlank
        public String assignee;
    }

    public static final class AddCommentRequest {
        public String commentId;
        @NotBlank
        public String content;
    }

    public static final class TagTaskRequest {
        @NotBlank
        public String tag;
    }

    public static final class UpdateTaskDescriptionRequest {
        public String description;
    }

    public record TaskIdResponse(String taskId) {
    }

    private AssigneeId toAssigneeIdOrNull(String rawAssignee) {
        if (rawAssignee == null || rawAssignee.isBlank()) {
            return null;
        }
        if (!userReadModels.exists(rawAssignee)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown assignee userId: " + rawAssignee);
        }
        return AssigneeId.of(rawAssignee);
    }
}
