package com.menglang.ticket.domain.port.out;

import com.menglang.ticket.domain.model.Company;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CompanyRepositoryPort {
    Flux<Company> findAll();
    Mono<Company> findById(String id);
    Mono<Company> findByCompanyId(Long companyId);
    Mono<Company> findByCode(String code);
    Mono<Company> save(Company company);
    Mono<Long> count();
}
