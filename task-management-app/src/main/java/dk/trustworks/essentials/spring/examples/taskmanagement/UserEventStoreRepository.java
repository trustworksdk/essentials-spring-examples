package dk.trustworks.essentials.spring.examples.taskmanagement;

import dk.trustworks.essentials.components.eventsourced.aggregates.stateful.StatefulAggregateRepository;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.ConfigurableEventStore;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.eventstream.AggregateType;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.persistence.table_per_aggregate_type.SeparateTablePerAggregateEventStreamConfiguration;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.User;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.UserEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static dk.trustworks.essentials.components.eventsourced.aggregates.stateful.StatefulAggregateInstanceFactory.reflectionBasedAggregateRootFactory;

@Component
public class UserEventStoreRepository {
    public static final AggregateType AGGREGATE_TYPE = AggregateType.of("TaskManagementUser");

    private final StatefulAggregateRepository<UserId, UserEvent, User> repository;

    public UserEventStoreRepository(ConfigurableEventStore<SeparateTablePerAggregateEventStreamConfiguration> eventStore) {
        repository = StatefulAggregateRepository.from(eventStore,
                                                      AGGREGATE_TYPE,
                                                      reflectionBasedAggregateRootFactory(),
                                                      User.class);
    }

    public Optional<User> findUser(UserId userId) {
        return repository.tryLoad(userId);
    }

    public void save(User user) {
        repository.save(user);
    }
}
