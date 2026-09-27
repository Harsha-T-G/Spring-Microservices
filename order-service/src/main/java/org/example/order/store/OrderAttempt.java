package org.example.order.store;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.example.order.dto.CreateOrderRequest;

@RequiredArgsConstructor
public class OrderAttempt {
    @Getter
    private final UUID id;
    @Getter
    private final Instant createdAt;
    private final CreateOrderRequest request;
    private final ReentrantLock lock;

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
