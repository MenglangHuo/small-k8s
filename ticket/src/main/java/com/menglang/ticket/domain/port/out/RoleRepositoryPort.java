package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleRepositoryPort {
    Flux<Role> findByCompanyIdOrIsSystemTrue(Long companyId);
    Mono<Role> findById(String id);
    Mono<Role> findByCodeAndCompanyId(String code, Long companyId);
    Mono<Role> save(Role role);
    Mono<Void> deleteById(String id);
    Mono<Long> count();
}
