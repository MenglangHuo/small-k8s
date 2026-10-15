package com.menglang.ticket.domain.port.in;

import reactor.core.publisher.Mono;

public interface OutboxUseCase {
    Mono<Void> processPendingOutbox();
}
