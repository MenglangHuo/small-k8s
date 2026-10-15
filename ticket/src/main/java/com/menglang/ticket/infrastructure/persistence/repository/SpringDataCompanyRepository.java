package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.CompanyDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataCompanyRepository extends ReactiveMongoRepository<CompanyDocument, String> {
    Mono<CompanyDocument> findByCompanyId(Long companyId);
    Mono<CompanyDocument> findByCode(String code);
}
