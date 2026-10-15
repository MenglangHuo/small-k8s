package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.ApiClientDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataApiClientRepository extends ReactiveMongoRepository<ApiClientDocument, String> {
    Mono<ApiClientDocument> findByIdAndCompanyId(String id, Long companyId);
    Mono<ApiClientDocument> findByClientId(String clientId);
    Flux<ApiClientDocument> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
