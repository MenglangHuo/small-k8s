package com.menglang.ticket.infrastructure.persistence.repository;

import com.menglang.ticket.infrastructure.persistence.document.UserDocument;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface SpringDataUserRepository extends ReactiveMongoRepository<UserDocument, String> {

    Mono<UserDocument> findByIdAndCompanyId(String id, Long companyId);

    Mono<UserDocument> findByUserIdAndCompanyId(Long userId, Long companyId);

    Mono<UserDocument> findByEmail(String email);

    Mono<UserDocument> findByUsername(String username);

    @Query("{ 'company_id': ?0, 'is_active': true }")
    Flux<UserDocument> findActiveByCompanyId(Long companyId);
}
