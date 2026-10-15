package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.Company;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface CompanyUseCase {
    Flux<Company> listCompanies();
    Mono<Company> getCompanyById(String id);
    Mono<Company> getCompanyByCompanyId(Long companyId);
    Mono<Company> createCompany(Company company);
    Mono<Company> updateCompany(String id, Map<String, Object> updates);
    Mono<Company> toggleStatus(String id);
}
