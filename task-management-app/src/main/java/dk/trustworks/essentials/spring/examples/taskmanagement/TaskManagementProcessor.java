package dk.trustworks.essentials.spring.examples.taskmanagement;

import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.eventstream.AggregateType;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.processor.EventProcessorDependencies;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.processor.InTransactionEventProcessor;
import dk.trustworks.essentials.components.foundation.messaging.MessageHandler;
import dk.trustworks.essentials.reactive.command.CmdHandler;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AddComment;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.AssignTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CompleteTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.ReopenTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.RegisterUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.StartTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.TagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UntagTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UnassignTask;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.UpdateTaskDescription;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.User;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Task;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.CommentAdded;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskAssigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCompleted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskReopened;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskStarted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskTagged;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUnassigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUntagged;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskDescriptionUpdated;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.TaskReadModels;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserReadModels;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserRegistered;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskManagementProcessor extends InTransactionEventProcessor {
    private final TaskEventStoreRepository taskEventStoreRepository;
    private final UserEventStoreRepository userEventStoreRepository;
    private final TaskReadModels taskReadModels;
    private final UserReadModels userReadModels;

    public TaskManagementProcessor(TaskEventStoreRepository taskEventStoreRepository,
                                   UserEventStoreRepository userEventStoreRepository,
                                   TaskReadModels taskReadModels,
                                   UserReadModels userReadModels,
                                   EventProcessorDependencies eventProcessorDependencies) {
        super(eventProcessorDependencies, true);
        this.taskEventStoreRepository = taskEventStoreRepository;
        this.userEventStoreRepository = userEventStoreRepository;
        this.taskReadModels = taskReadModels;
        this.userReadModels = userReadModels;
    }

    @Override
    public String getProcessorName() {
        return "TaskManagementProcessor";
    }

    @Override
    protected List<AggregateType> reactsToEventsRelatedToAggregateTypes() {
        return List.of(TaskEventStoreRepository.AGGREGATE_TYPE, UserEventStoreRepository.AGGREGATE_TYPE);
    }

    @CmdHandler
    public void handle(CreateUser cmd) {
        if (userEventStoreRepository.findUser(cmd.userId()).isPresent()) {
            return;
        }

        var user = new User(cmd);
        if (!user.getUncommittedChanges().isEmpty()) {
            userEventStoreRepository.save(user);
        }
    }

    @CmdHandler
    public void handle(RegisterUser cmd) {
        userEventStoreRepository.findUser(cmd.userId()).ifPresent(user -> user.register(cmd));
    }

    @CmdHandler
    public void handle(CreateTask cmd) {
        if (cmd.assignee() != null && !hasUser(cmd.assignee().toString())) {
            return;
        }

        if (taskEventStoreRepository.findTask(cmd.taskId()).isPresent()) {
            return;
        }

        var task = new Task(cmd);
        if (!task.getUncommittedChanges().isEmpty()) {
            taskEventStoreRepository.save(task);
        }
    }

    @CmdHandler
    public void handle(StartTask cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.start(cmd));
    }

    @CmdHandler
    public void handle(CompleteTask cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.complete(cmd));
    }

    @CmdHandler
    public void handle(ReopenTask cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.reopen(cmd));
    }

    @CmdHandler
    public void handle(AssignTask cmd) {
        if (!hasUser(cmd.assignee().toString())) {
            return;
        }
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.assign(cmd));
    }

    @CmdHandler
    public void handle(UnassignTask cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.unassign(cmd));
    }

    @CmdHandler
    public void handle(AddComment cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.addComment(cmd));
    }

    @CmdHandler
    public void handle(TagTask cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.tag(cmd));
    }

    @CmdHandler
    public void handle(UntagTask cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.untag(cmd));
    }

    @CmdHandler
    public void handle(UpdateTaskDescription cmd) {
        taskEventStoreRepository.findTask(cmd.taskId()).ifPresent(task -> task.updateDescription(cmd));
    }

    @MessageHandler
    void handle(TaskCreated event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskStarted event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskCompleted event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskReopened event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskAssigned event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskUnassigned event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(CommentAdded event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskTagged event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskUntagged event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(TaskDescriptionUpdated event) {
        taskReadModels.on(event);
    }

    @MessageHandler
    void handle(UserCreated event) {
        userReadModels.on(event);
    }

    @MessageHandler
    void handle(UserRegistered event) {
        userReadModels.on(event);
    }

    private boolean hasUser(String userId) {
        return userEventStoreRepository.findUser(UserId.of(userId)).isPresent();
    }
}
