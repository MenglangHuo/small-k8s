package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.TicketComment;
import com.menglang.ticket.domain.port.out.CommentRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.TicketCommentDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataCommentRepository;
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
public class MongoCommentRepositoryAdapter implements CommentRepositoryPort {

    private final SpringDataCommentRepository commentRepository;
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<TicketComment> save(TicketComment comment) {
        TicketCommentDocument doc = TicketDocMapper.toDocument(comment);
        return commentRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<TicketComment> findById(String commentId, String ticketId) {
        return commentRepository.findByIdAndTicketIdAndIsDeletedFalse(commentId, ticketId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<TicketComment> findByTicketId(String ticketId, boolean includeInternal) {
        if (includeInternal) {
            return commentRepository.findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(ticketId)
                    .map(TicketDocMapper::toDomain);
        } else {
            return commentRepository.findByTicketIdAndIsInternalFalseAndIsDeletedFalseOrderByCreatedAtAsc(ticketId)
                    .map(TicketDocMapper::toDomain);
        }
    }

    @Override
    public Mono<Boolean> softDelete(String commentId, String ticketId, Long deletedBy) {
        Query query = Query.query(Criteria.where("_id").is(commentId).and("ticket_id").is(ticketId));
        Update update = new Update()
                .set("is_deleted", true)
                .set("updated_by", deletedBy)
                .set("updated_at", Instant.now());

        return mongoTemplate.updateFirst(query, update, TicketCommentDocument.class)
                .map(res -> res.getModifiedCount() > 0);
    }
}
