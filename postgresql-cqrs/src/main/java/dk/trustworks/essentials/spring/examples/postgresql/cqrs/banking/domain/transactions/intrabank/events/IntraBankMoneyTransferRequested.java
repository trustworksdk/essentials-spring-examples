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

package dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.transactions.intrabank.events;

import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.commands.RequestIntraBankMoneyTransfer;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.account.AccountId;
import dk.trustworks.essentials.spring.examples.postgresql.cqrs.banking.domain.transactions.intrabank.TransferLifeCycleStatus;
import dk.trustworks.essentials.types.Amount;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IntraBankMoneyTransferRequested extends IntraBankMoneyTransferEvent {
    public final AccountId               fromAccount;
    public final AccountId               toAccount;
    public final Amount                  amount;
    public final TransferLifeCycleStatus status;

    public IntraBankMoneyTransferRequested(@NonNull RequestIntraBankMoneyTransfer cmd) {
        super(cmd.transactionId);
        fromAccount = cmd.fromAccount;
        toAccount = cmd.toAccount;
        amount = cmd.amount;
        status = TransferLifeCycleStatus.REQUESTED;
    }
}
