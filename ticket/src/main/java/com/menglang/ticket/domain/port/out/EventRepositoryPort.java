package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.TicketEvent;
import reactor.core.publisher.Mono;

public interface EventRepositoryPort {
    Mono<TicketEvent> record(TicketEvent event);
}
