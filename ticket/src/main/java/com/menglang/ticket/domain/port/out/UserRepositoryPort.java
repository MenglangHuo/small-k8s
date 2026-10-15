package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Flux<User> findByCompanyIdWithPagination(Long companyId, int skip, int limit, String search);
    Mono<Long> countByCompanyId(Long companyId, String search);
    Flux<User> findAgentsByCompanyId(Long companyId);
    Mono<User> findByIdAndCompanyId(String id, Long companyId);
    Mono<User> findByUserIdAndCompanyId(Long userId, Long companyId);
    Mono<User> findByEmail(String email);
    Mono<User> findByUsername(String username);
    Mono<User> save(User user);
    Mono<Long> count();
}
