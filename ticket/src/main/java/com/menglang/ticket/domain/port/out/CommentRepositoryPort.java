package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.TicketComment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentRepositoryPort {
    Mono<TicketComment> save(TicketComment comment);
    Mono<TicketComment> findById(String commentId, String ticketId);
    Flux<TicketComment> findByTicketId(String ticketId, boolean includeInternal);
    Mono<Boolean> softDelete(String commentId, String ticketId, Long deletedBy);
}
