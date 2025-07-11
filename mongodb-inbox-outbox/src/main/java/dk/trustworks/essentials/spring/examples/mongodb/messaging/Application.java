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

package dk.trustworks.essentials.spring.examples.mongodb.messaging;

import dk.trustworks.essentials.components.boot.autoconfigure.mongodb.AdditionalCharSequenceTypesSupported;
import dk.trustworks.essentials.components.boot.autoconfigure.mongodb.AdditionalConverters;
import dk.trustworks.essentials.spring.examples.mongodb.messaging.shipping.OrderId;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.Jsr310Converters;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    @Bean
    AdditionalCharSequenceTypesSupported additionalCharSequenceTypesSupported() {
        return new AdditionalCharSequenceTypesSupported(OrderId.class);
    }

    @Bean
    AdditionalConverters additionalGenericConverters() {
        return new AdditionalConverters(Jsr310Converters.StringToDurationConverter.INSTANCE,
                                        Jsr310Converters.DurationToStringConverter.INSTANCE);
    }
}
