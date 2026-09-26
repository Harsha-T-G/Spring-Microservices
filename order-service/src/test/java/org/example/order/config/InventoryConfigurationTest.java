package org.example.order.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryConfigurationTest {
    @ParameterizedTest
    @ValueSource(strings = {"inventory.connect-timeout-ms=501", "inventory.response-timeout-ms=1001",
            "inventory.connect-timeout-ms=0", "inventory.response-timeout-ms=0", "inventory.retry.max-attempts=3",
            "inventory.retry.max-attempts=0", "orders.in-progress-wait-ms=0"})
    void givenUnsafeTimeoutOrRetrySetting_whenStarting_thenConfigurationIsRejected(String override) {
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class, ValidationAutoConfiguration.class)
                .withPropertyValues("inventory.base-url=http://localhost:8081", "inventory.connect-timeout-ms=500",
                        "inventory.response-timeout-ms=1000", "inventory.retry.max-attempts=2",
                        "inventory.retry.wait-ms=100", "orders.in-progress-wait-ms=4000")
                .withPropertyValues(override)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({InventoryProperties.class, RetryProperties.class, OrderProperties.class})
    static class PropertiesConfiguration {
    }
}
