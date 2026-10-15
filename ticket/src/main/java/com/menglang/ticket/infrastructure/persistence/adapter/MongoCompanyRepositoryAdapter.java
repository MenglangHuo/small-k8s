package com.menglang.ticket.infrastructure.persistence.adapter;

import com.menglang.ticket.domain.model.Company;
import com.menglang.ticket.domain.port.out.CompanyRepositoryPort;
import com.menglang.ticket.infrastructure.persistence.document.CompanyDocument;
import com.menglang.ticket.infrastructure.persistence.repository.SpringDataCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MongoCompanyRepositoryAdapter implements CompanyRepositoryPort {

    private final SpringDataCompanyRepository repository;

    @Override
    public Flux<Company> findAll() {
        return repository.findAll().map(this::toDomain);
    }

    @Override
    public Mono<Company> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Mono<Company> findByCompanyId(Long companyId) {
        return repository.findByCompanyId(companyId).map(this::toDomain);
    }

    @Override
    public Mono<Company> findByCode(String code) {
        return repository.findByCode(code).map(this::toDomain);
    }

    @Override
    public Mono<Company> save(Company company) {
        CompanyDocument doc = toDocument(company);
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(Instant.now());
        }
        doc.setUpdatedAt(Instant.now());
        return repository.save(doc).map(this::toDomain);
    }

    @Override
    public Mono<Long> count() {
        return repository.count();
    }

    private Company toDomain(CompanyDocument doc) {
        if (doc == null) return null;
        return Company.builder()
                .id(doc.getId())
                .companyId(doc.getCompanyId())
                .name(doc.getName())
                .code(doc.getCode())
                .domain(doc.getDomain())
                .isActive(doc.getIsActive())
                .metadata(doc.getMetadata())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private CompanyDocument toDocument(Company domain) {
        if (domain == null) return null;
        return CompanyDocument.builder()
                .id(domain.getId())
                .companyId(domain.getCompanyId())
                .name(domain.getName())
                .code(domain.getCode())
                .domain(domain.getDomain())
                .isActive(domain.getIsActive())
                .metadata(domain.getMetadata())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
