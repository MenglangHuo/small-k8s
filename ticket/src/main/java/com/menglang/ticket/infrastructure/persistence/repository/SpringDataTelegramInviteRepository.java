package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.TelegramInviteDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface SpringDataTelegramInviteRepository extends ReactiveMongoRepository<TelegramInviteDocument, String> {
    Mono<TelegramInviteDocument> findByTokenAndIsUsedFalseAndExpiresAtGreaterThan(String token, Instant now);
    Mono<Long> countByCompanyId(Long companyId);
}
