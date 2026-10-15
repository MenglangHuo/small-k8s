package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.exception.ResourceNotFoundException;
import com.menglang.ticket.domain.model.Company;
import com.menglang.ticket.domain.port.in.CompanyUseCase;
import com.menglang.ticket.domain.port.out.CompanyRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyApplicationService implements CompanyUseCase {

    private final CompanyRepositoryPort companyRepositoryPort;

    @Override
    public Flux<Company> listCompanies() {
        return companyRepositoryPort.findAll();
    }

    @Override
    public Mono<Company> getCompanyById(String id) {
        return companyRepositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Company not found with id: " + id)));
    }

    @Override
    public Mono<Company> getCompanyByCompanyId(Long companyId) {
        return companyRepositoryPort.findByCompanyId(companyId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Company not found with company_id: " + companyId)));
    }

    @Override
    public Mono<Company> createCompany(Company company) {
        if (company.getCode() == null || company.getCode().isBlank()) {
            return Mono.error(new IllegalArgumentException("Company code is required"));
        }
        if (company.getName() == null || company.getName().isBlank()) {
            return Mono.error(new IllegalArgumentException("Company name is required"));
        }

        return companyRepositoryPort.findByCode(company.getCode().trim().toUpperCase())
                .flatMap(existing -> Mono.<Company>error(new IllegalArgumentException("Company code already exists: " + existing.getCode())))
                .switchIfEmpty(Mono.defer(() -> {
                    company.setCode(company.getCode().trim().toUpperCase());
                    if (company.getCompanyId() == null) {
                        return companyRepositoryPort.count()
                                .flatMap(count -> {
                                    company.setCompanyId(count + 1);
                                    return companyRepositoryPort.save(company);
                                });
                    }
                    return companyRepositoryPort.save(company);
                }));
    }

    @Override
    public Mono<Company> updateCompany(String id, Map<String, Object> updates) {
        return getCompanyById(id)
                .flatMap(company -> {
                    if (updates.containsKey("name") && updates.get("name") != null) {
                        company.setName((String) updates.get("name"));
                    }
                    if (updates.containsKey("domain") && updates.get("domain") != null) {
                        company.setDomain((String) updates.get("domain"));
                    }
                    if (updates.containsKey("is_active") && updates.get("is_active") != null) {
                        company.setIsActive(Boolean.parseBoolean(String.valueOf(updates.get("is_active"))));
                    }
                    if (updates.containsKey("metadata") && updates.get("metadata") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> meta = (Map<String, Object>) updates.get("metadata");
                        company.setMetadata(meta);
                    }
                    return companyRepositoryPort.save(company);
                });
    }

    @Override
    public Mono<Company> toggleStatus(String id) {
        return getCompanyById(id)
                .flatMap(company -> {
                    company.setIsActive(!Boolean.TRUE.equals(company.getIsActive()));
                    return companyRepositoryPort.save(company);
                });
    }
}
