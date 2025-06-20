/*
 * Copyright 2021-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking;

import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.bus.CommitStage;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.bus.EventStoreEventBus;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.bus.PersistedEvents;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.types.EventOrder;
import dk.trustworks.essentials.components.eventsourced.eventstore.postgresql.types.EventType;
import dk.trustworks.essentials.components.foundation.transaction.UnitOfWork;
import dk.trustworks.essentials.components.foundation.transaction.UnitOfWorkFactory;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.Application;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.TestConfiguration;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.account.AccountId;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.account.AccountNumber;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.account.Accounts;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.account.events.AccountDeposited;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.account.events.AccountOpened;
import dk.trustworks.essentials.types.Amount;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class, TestConfiguration.class})
@Testcontainers
@Slf4j
@DirtiesContext
public class AccountsIT {
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("test")
            .withPassword("test")
            .withUsername("test");

    @Container
    static  org.testcontainers.kafka.KafkaContainer       kafkaContainer = new org.testcontainers.kafka.KafkaContainer("apache/kafka-native:latest")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Autowired
    private Accounts accounts;

    @Autowired
    private EventStoreEventBus eventStoreEventBus;

    @Autowired
    private UnitOfWorkFactory<? extends UnitOfWork> unitOfWorkFactory;

    @Test
    void verify_events_are_published_on_the_eventstore_local_bus() {
        var account1Id    = AccountId.random();
        var transactionId = TransactionId.random();
        var depositAmount = Amount.of("100");

        var allPersistedEvents = new ArrayList<PersistedEvents>();
        eventStoreEventBus.addSyncSubscriber(event -> {
            if (event instanceof PersistedEvents) {
                var persistedEvents = (PersistedEvents) event;
                if (persistedEvents.commitStage == CommitStage.BeforeCommit && persistedEvents.events.size() > 0) {
                    allPersistedEvents.add(persistedEvents);
                }
            }
        });

        unitOfWorkFactory.usingUnitOfWork(unitOfWork -> {
            var account1 = accounts.openNewAccount(account1Id,
                                                   AccountNumber.of("001123456"));
            account1.depositToday(depositAmount, transactionId);
        });

        var account1 = unitOfWorkFactory.withUnitOfWork(unitOfWork -> accounts.getAccount(account1Id));
        assertThat(account1.getBalance()).isEqualTo(depositAmount);

        assertThat(allPersistedEvents.size()).isEqualTo(1);
        var events = allPersistedEvents.get(0).events;
        assertThat(events.size()).isEqualTo(2);
        assertThat((CharSequence) events.get(0).aggregateType()).isEqualTo(Accounts.AGGREGATE_TYPE);
        assertThat((CharSequence) events.get(0).aggregateId()).isEqualTo(account1Id);
        assertThat(events.get(0).eventOrder()).isEqualTo(EventOrder.of(0));
        assertThat((CharSequence) events.get(0).event().getEventType().get()).isEqualTo(EventType.of(AccountOpened.class));
        assertThat((CharSequence) events.get(1).aggregateType()).isEqualTo(Accounts.AGGREGATE_TYPE);
        assertThat((CharSequence) events.get(1).aggregateId()).isEqualTo(account1Id);
        assertThat(events.get(1).eventOrder()).isEqualTo(EventOrder.of(1));
        assertThat((CharSequence) events.get(1).event().getEventType().get()).isEqualTo(EventType.of(AccountDeposited.class));
    }
}
