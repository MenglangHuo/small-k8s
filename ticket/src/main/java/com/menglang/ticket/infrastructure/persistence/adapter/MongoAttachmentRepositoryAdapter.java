package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.TicketAttachment;
import com.menglang.ticket.domain.port.out.AttachmentRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TicketAttachmentDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MongoAttachmentRepositoryAdapter implements AttachmentRepositoryPort {

    private final SpringDataAttachmentRepository attachmentRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<TicketAttachment> save(TicketAttachment attachment) {
        TicketAttachmentDocument doc = TicketDocMapper.toDocument(attachment);
        return attachmentRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<TicketAttachment> findById(String id, String ticketId) {
        return attachmentRepository.findByIdAndTicketIdAndIsDeletedFalse(id, ticketId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<TicketAttachment> findByTicketId(String ticketId) {
        return attachmentRepository.findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(ticketId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<TicketAttachment> findByCommentId(String commentId) {
        return attachmentRepository.findByCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(commentId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Boolean> softDelete(String id, String ticketId) {
        Query query = Query.query(Criteria.where("_id").is(id).and("ticket_id").is(ticketId));
        Update update = new Update()
                .set("is_deleted", true)
                .set("updated_at", Instant.now());

        return mongoTemplate.updateFirst(query, update, TicketAttachmentDocument.class)
                .map(res -> res.getModifiedCount() > 0);
    }
}
