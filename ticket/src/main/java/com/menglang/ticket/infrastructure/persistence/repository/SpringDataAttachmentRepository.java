package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.TicketAttachmentDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataAttachmentRepository extends ReactiveMongoRepository<TicketAttachmentDocument, String> {
    Flux<TicketAttachmentDocument> findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(String ticketId);
    Flux<TicketAttachmentDocument> findByCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(String commentId);
    Mono<TicketAttachmentDocument> findByIdAndTicketIdAndIsDeletedFalse(String id, String ticketId);
}
