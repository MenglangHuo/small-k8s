package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.NotificationOutboxDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Repository
public interface SpringDataOutboxRepository extends ReactiveMongoRepository<NotificationOutboxDocument, String> {
    Flux<NotificationOutboxDocument> findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            String status, Instant availableAt, Pageable pageable);
}
