package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.TicketSequenceDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSequenceRepository extends ReactiveMongoRepository<TicketSequenceDocument, Long> {
}
