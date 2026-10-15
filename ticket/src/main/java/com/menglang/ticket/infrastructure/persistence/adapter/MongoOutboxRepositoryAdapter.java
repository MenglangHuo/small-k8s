package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.NotificationOutbox;
import com.menglang.ticket.domain.port.out.OutboxRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.NotificationOutboxDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class MongoOutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository outboxRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<NotificationOutbox> enqueue(NotificationOutbox outbox) {
        NotificationOutboxDocument doc = TicketDocMapper.toDocument(outbox);
        return outboxRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<NotificationOutbox> findPending(int limit) {
        return outboxRepository.findByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                "pending", Instant.now(), PageRequest.of(0, limit))
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<NotificationOutbox> markProcessing(String id) {
        Query query = Query.query(Criteria.where("_id").is(id).and("status").is("pending"));
        Update update = new Update()
                .set("status", "processing")
                .inc("attempts", 1)
                .set("updated_at", Instant.now());

        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), NotificationOutboxDocument.class)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Void> markSent(String id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("status", "sent")
                .set("sent_at", Instant.now())
                .set("updated_at", Instant.now());

        return mongoTemplate.updateFirst(query, update, NotificationOutboxDocument.class).then();
    }

    @Override
    public Mono<Void> markFailed(String id, String error, int maxAttempts) {
        Query query = Query.query(Criteria.where("_id").is(id));
        
        return mongoTemplate.findOne(query, NotificationOutboxDocument.class)
                .flatMap(doc -> {
                    Update update = new Update()
                            .set("last_error", error)
                            .set("updated_at", Instant.now());
                    
                    if (doc.getAttempts() >= maxAttempts) {
                        update.set("status", "dead_letter");
                    } else {
                        // Exponential backoff: 30s * 2^(attempts-1)
                        long backoffSeconds = Math.min((long) Math.pow(2, doc.getAttempts()) * 30L, 3600L);
                        update.set("status", "pending")
                              .set("available_at", Instant.now().plus(backoffSeconds, ChronoUnit.SECONDS));
                    }
                    return mongoTemplate.updateFirst(query, update, NotificationOutboxDocument.class);
                }).then();
    }
}
