package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.TelegramSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TelegramSubscriberRepositoryPort {
    Mono<TelegramSubscriber> save(TelegramSubscriber subscriber);
    Mono<TelegramSubscriber> findByChatIdAndCompanyId(String chatId, Long companyId);
    Flux<TelegramSubscriber> findActiveByCompanyId(Long companyId);
    Flux<TelegramSubscriber> findByCompanyId(Long companyId, int page, int perPage);
    Mono<Long> countByCompanyId(Long companyId, Boolean activeOnly);
    Mono<Boolean> remove(String chatId, Long companyId);
}
