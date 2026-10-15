package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.RoleDocument;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataRoleRepository extends ReactiveMongoRepository<RoleDocument, String> {
    
    @Query("{ '$or': [ { 'company_id': ?0 }, { 'is_system': true } ] }")
    Flux<RoleDocument> findByCompanyIdOrIsSystemTrue(Long companyId);

    Mono<RoleDocument> findByCodeAndCompanyId(String code, Long companyId);
}
