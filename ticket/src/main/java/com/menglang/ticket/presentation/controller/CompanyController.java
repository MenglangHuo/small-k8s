package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.Company;
import com.menglang.ticket.domain.port.in.CompanyUseCase;
import com.menglang.ticket.presentation.request.CreateCompanyRequest;
import com.menglang.ticket.presentation.request.UpdateCompanyRequest;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.CompanyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyUseCase companyUseCase;

    @GetMapping
    public Mono<ApiResponse<List<CompanyResponse>>> index() {
        return companyUseCase.listCompanies()
                .map(CompanyResponse::fromDomain)
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<CompanyResponse>> store(@Valid @RequestBody CreateCompanyRequest request) {
        Company company = Company.builder()
                .name(request.getName())
                .code(request.getCode())
                .domain(request.getDomain())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .metadata(request.getMetadata())
                .build();

        return companyUseCase.createCompany(company)
                .map(created -> ApiResponse.success(CompanyResponse.fromDomain(created), "Company created successfully."));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<CompanyResponse>> show(@PathVariable String id) {
        return companyUseCase.getCompanyById(id)
                .map(company -> ApiResponse.success(CompanyResponse.fromDomain(company)));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<CompanyResponse>> update(
            @PathVariable String id,
            @RequestBody UpdateCompanyRequest request) {
        return companyUseCase.updateCompany(id, request.toMap())
                .map(updated -> ApiResponse.success(CompanyResponse.fromDomain(updated), "Company updated successfully."));
    }

    @PatchMapping("/{id}/status")
    public Mono<ApiResponse<CompanyResponse>> toggleStatus(@PathVariable String id) {
        return companyUseCase.toggleStatus(id)
                .map(toggled -> ApiResponse.success(CompanyResponse.fromDomain(toggled), "Company status toggled successfully."));
    }
}
