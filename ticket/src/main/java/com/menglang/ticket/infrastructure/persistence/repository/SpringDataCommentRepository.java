package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.TicketCommentDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataCommentRepository extends ReactiveMongoRepository<TicketCommentDocument, String> {
    Flux<TicketCommentDocument> findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(String ticketId);
    Flux<TicketCommentDocument> findByTicketIdAndIsInternalFalseAndIsDeletedFalseOrderByCreatedAtAsc(String ticketId);
    Mono<TicketCommentDocument> findByIdAndTicketIdAndIsDeletedFalse(String id, String ticketId);
}
