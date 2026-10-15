package com.menglang.ticket.domain.port.out;

import reactor.core.publisher.Mono;

public interface SequenceRepositoryPort {
    Mono<String> nextTicketKey(Long companyId, String prefix);
}
