package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.TicketAttachment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AttachmentRepositoryPort {
    Mono<TicketAttachment> save(TicketAttachment attachment);
    Mono<TicketAttachment> findById(String id, String ticketId);
    Flux<TicketAttachment> findByTicketId(String ticketId);
    Flux<TicketAttachment> findByCommentId(String commentId);
    Mono<Boolean> softDelete(String id, String ticketId);
}
