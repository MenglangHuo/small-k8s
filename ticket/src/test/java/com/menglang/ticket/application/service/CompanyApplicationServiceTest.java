package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.model.Company;
import com.menglang.ticket.domain.port.out.CompanyRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyApplicationServiceTest {

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    private CompanyApplicationService companyApplicationService;

    @BeforeEach
    void setUp() {
        companyApplicationService = new CompanyApplicationService(companyRepositoryPort);
    }

    @Test
    @DisplayName("createCompany successfully creates company with auto companyId")
    void testCreateCompany() {
        Company newCompany = Company.builder()
                .name("Test Company")
                .code("TEST")
                .domain("test.com")
                .build();

        when(companyRepositoryPort.findByCode("TEST")).thenReturn(Mono.empty());
        when(companyRepositoryPort.count()).thenReturn(Mono.just(1L));
        when(companyRepositoryPort.save(any(Company.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        Mono<Company> result = companyApplicationService.createCompany(newCompany);

        StepVerifier.create(result)
                .assertNext(created -> {
                    assertEquals("TEST", created.getCode());
                    assertEquals("Test Company", created.getName());
                    assertEquals(2L, created.getCompanyId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createCompany fails when company code already exists")
    void testCreateCompanyDuplicateCode() {
        Company existing = Company.builder().code("TEST").build();
        when(companyRepositoryPort.findByCode("TEST")).thenReturn(Mono.just(existing));

        Company newCompany = Company.builder().name("Test").code("TEST").build();

        Mono<Company> result = companyApplicationService.createCompany(newCompany);

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
