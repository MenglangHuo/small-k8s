package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.ApiClient;
import com.menglang.ticket.domain.port.out.ApiClientRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.ApiClientDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataApiClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MongoApiClientRepositoryAdapter implements ApiClientRepositoryPort {

    private final SpringDataApiClientRepository apiClientRepository;

    @Override
    public Mono<ApiClient> save(ApiClient apiClient) {
        ApiClientDocument doc = TicketDocMapper.toDocument(apiClient);
        return apiClientRepository.save(doc)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<ApiClient> findById(String id, Long companyId) {
        return apiClientRepository.findByIdAndCompanyId(id, companyId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<ApiClient> findByClientId(String clientId) {
        return apiClientRepository.findByClientId(clientId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Flux<ApiClient> findByCompanyId(Long companyId) {
        return apiClientRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .map(TicketDocMapper::toDomain);
    }

    @Override
    public Mono<Boolean> delete(String id, Long companyId) {
        return apiClientRepository.findByIdAndCompanyId(id, companyId)
                .flatMap(doc -> apiClientRepository.delete(doc).thenReturn(true))
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Long> count() {
        return apiClientRepository.count();
    }
}
