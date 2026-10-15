package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.NotificationOutbox;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxRepositoryPort {
    Mono<NotificationOutbox> enqueue(NotificationOutbox outbox);
    Flux<NotificationOutbox> findPending(int limit);
    Mono<NotificationOutbox> markProcessing(String id);
    Mono<Void> markSent(String id);
    Mono<Void> markFailed(String id, String error, int maxAttempts);
}
