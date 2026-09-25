package org.example.order.store;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;

import org.example.order.dto.CreateOrderRequest;

public class OrderAttempt {
    @Getter
    private final UUID id = UUID.randomUUID();
    @Getter
    private final Instant createdAt = Instant.now();
    private final CreateOrderRequest request;
    private final ReentrantLock lock = new ReentrantLock();

    public OrderAttempt(CreateOrderRequest request) {
        this.request = request;
    }

    public boolean matches(CreateOrderRequest candidate) {
        return request.equals(candidate);
    }

    public boolean acquire(long waitMillis) throws InterruptedException {
        return lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
    }

    public void release() {
        lock.unlock();
    }
}
