package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.TicketDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataTicketRepository extends ReactiveMongoRepository<TicketDocument, String> {
    Mono<TicketDocument> findByIdAndCompanyIdAndIsDeletedFalse(String id, Long companyId);
    Mono<TicketDocument> findByTicketKeyAndCompanyIdAndIsDeletedFalse(String ticketKey, Long companyId);
    Mono<TicketDocument> findByPublicIdAndCompanyIdAndIsDeletedFalse(String publicId, Long companyId);
}
