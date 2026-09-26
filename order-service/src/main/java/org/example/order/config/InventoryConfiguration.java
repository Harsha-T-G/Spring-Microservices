package org.example.order.config;

import java.net.http.HttpClient;
import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import org.example.order.gateway.InventoryFailure;

@Configuration
@Slf4j
@EnableConfigurationProperties({InventoryProperties.class, OrderProperties.class,
        RetryProperties.class, BreakerProperties.class})
public class InventoryConfiguration {
    @Bean
    public Retry inventoryRetry(RetryProperties properties) {
        Retry retry = Retry.of("inventory", RetryConfig.custom()
                .maxAttempts(properties.maxAttempts()).waitDuration(Duration.ofMillis(properties.waitMs()))
                .retryOnException(failure -> failure instanceof InventoryFailure inventoryFailure
                        && inventoryFailure.getKind() == InventoryFailure.Kind.UNAVAILABLE)
                .build());
        retry.getEventPublisher().onRetry(event -> log.warn("inventoryRetry={} reason=INVENTORY_UNAVAILABLE",
                event.getNumberOfRetryAttempts()));
        return retry;
    }

    @Bean
    public CircuitBreaker inventoryCircuitBreaker(BreakerProperties properties) {
        CircuitBreaker breaker = CircuitBreaker.of("inventory", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(4).minimumNumberOfCalls(4).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(properties.openWaitMs()))
                .permittedNumberOfCallsInHalfOpenState(1)
                .ignoreException(failure -> failure instanceof InventoryFailure inventoryFailure
                        && (inventoryFailure.getKind() == InventoryFailure.Kind.BUSINESS
                        || inventoryFailure.getKind() == InventoryFailure.Kind.CONFLICT))
                .build());
        breaker.getEventPublisher().onStateTransition(event ->
                log.info("inventoryCircuitTransition={}", event.getStateTransition()));
        return breaker;
    }

    @Bean(destroyMethod = "shutdownNow")
    public HttpClient inventoryHttpClient(InventoryProperties properties) {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs())).build();
    }

    @Bean
    public RestClient inventoryRestClient(RestClient.Builder builder, HttpClient inventoryHttpClient,
                                         InventoryProperties properties) {
        var factory = new JdkClientHttpRequestFactory(inventoryHttpClient);
        factory.setReadTimeout(Duration.ofMillis(properties.responseTimeoutMs()));
        return builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }
}
