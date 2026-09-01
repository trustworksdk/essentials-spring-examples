package dk.trustworks.essentials.spring.examples.taskmanagement.projection;

import dk.trustworks.essentials.components.eventsourced.aggregates.projection.AnnotationBasedInMemoryProjector;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.ConfigurableEventStore;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.persistence.table_per_aggregate_type.SeparateTablePerAggregateEventStreamConfiguration;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.transaction.EventStoreUnitOfWorkFactory;
import dk.trustworks.essentials.spring.examples.taskmanagement.TaskEventStoreRepository;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskId;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskInMemoryProjectionService {
    private final ConfigurableEventStore<SeparateTablePerAggregateEventStreamConfiguration> eventStore;
    private final EventStoreUnitOfWorkFactory<?> unitOfWorkFactory;
    private final AnnotationBasedInMemoryProjector annotationBasedInMemoryProjector;

    public TaskInMemoryProjectionService(ConfigurableEventStore<SeparateTablePerAggregateEventStreamConfiguration> eventStore,
                                         EventStoreUnitOfWorkFactory<?> unitOfWorkFactory) {
        this.eventStore = eventStore;
        this.unitOfWorkFactory = unitOfWorkFactory;
        this.annotationBasedInMemoryProjector = new AnnotationBasedInMemoryProjector();
    }

    public Optional<TaskInMemoryProjection> project(TaskId taskId) {
        return unitOfWorkFactory.withUnitOfWork(() -> eventStore.inMemoryProjection(TaskEventStoreRepository.AGGREGATE_TYPE,
                                                                                     taskId,
                                                                                     TaskInMemoryProjection.class,
                                                                                     annotationBasedInMemoryProjector));
    }
}
