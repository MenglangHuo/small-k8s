package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.TelegramInvite;
import reactor.core.publisher.Mono;

public interface TelegramInviteRepositoryPort {
    Mono<TelegramInvite> save(TelegramInvite invite);
    Mono<TelegramInvite> findValidByToken(String token);
    Mono<Long> countByCompanyId(Long companyId);
}
