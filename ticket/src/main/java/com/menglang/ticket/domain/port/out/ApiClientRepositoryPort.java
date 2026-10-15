package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.ApiClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiClientRepositoryPort {
    Mono<ApiClient> save(ApiClient apiClient);
    Mono<ApiClient> findById(String id, Long companyId);
    Mono<ApiClient> findByClientId(String clientId);
    Flux<ApiClient> findByCompanyId(Long companyId);
    Mono<Boolean> delete(String id, Long companyId);
    Mono<Long> count();
}
