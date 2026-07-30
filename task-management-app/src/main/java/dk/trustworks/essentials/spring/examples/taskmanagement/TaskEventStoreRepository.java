package dk.trustworks.essentials.spring.examples.taskmanagement;

import dk.trustworks.essentials.components.eventsourced.aggregates.stateful.StatefulAggregateRepository;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.ConfigurableEventStore;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.eventstream.AggregateType;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.persistence.table_per_aggregate_type.SeparateTablePerAggregateEventStreamConfiguration;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Task;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static dk.trustworks.essentials.components.eventsourced.aggregates.stateful.StatefulAggregateInstanceFactory.reflectionBasedAggregateRootFactory;

@Component
public class TaskEventStoreRepository {
    public static final AggregateType AGGREGATE_TYPE = AggregateType.of("TaskManagementTask");

    private final StatefulAggregateRepository<TaskId, TaskEvent, Task> repository;

    public TaskEventStoreRepository(ConfigurableEventStore<SeparateTablePerAggregateEventStreamConfiguration> eventStore) {
        repository = StatefulAggregateRepository.from(eventStore,
                                                      AGGREGATE_TYPE,
                                                      reflectionBasedAggregateRootFactory(),
                                                      Task.class);
    }

    public Optional<Task> findTask(TaskId taskId) {
        return repository.tryLoad(taskId);
    }

    public void save(Task task) {
        repository.save(task);
    }
}
