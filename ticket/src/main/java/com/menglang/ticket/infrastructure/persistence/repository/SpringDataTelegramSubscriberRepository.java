package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.TelegramSubscriberDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataTelegramSubscriberRepository extends ReactiveMongoRepository<TelegramSubscriberDocument, String> {
    Mono<TelegramSubscriberDocument> findByTelegramChatIdAndCompanyId(String chatId, Long companyId);
    Flux<TelegramSubscriberDocument> findByCompanyIdAndIsActiveTrue(Long companyId);
    Flux<TelegramSubscriberDocument> findByCompanyIdOrderBySubscribedAtDesc(Long companyId, Pageable pageable);
    Mono<Long> countByCompanyId(Long companyId);
    Mono<Long> countByCompanyIdAndIsActiveTrue(Long companyId);
}
