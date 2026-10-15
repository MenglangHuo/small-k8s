package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.ApiClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface ApiClientUseCase {
    Flux<ApiClient> listApiClients(Long companyId);
    Mono<Map<String, Object>> createApiClient(Long companyId, ApiClient client);
    Mono<ApiClient> getApiClient(String id, Long companyId);
    Mono<Map<String, Object>> updateApiClient(String id, Long companyId, Map<String, Object> fields);
    Mono<Boolean> deleteApiClient(String id, Long companyId);
    Mono<ApiClient> toggleStatus(String id, Long companyId);
    Mono<Map<String, Object>> regenerateSecret(String id, Long companyId);
    Mono<ApiClient> authenticate(String clientId, String clientSecret);
}
