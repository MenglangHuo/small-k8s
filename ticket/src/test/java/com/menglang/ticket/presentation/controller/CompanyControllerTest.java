package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.Company;
import com.menglang.ticket.domain.port.in.CompanyUseCase;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.CompanyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CompanyUseCase companyUseCase;

    private CompanyController companyController;

    @BeforeEach
    void setUp() {
        companyController = new CompanyController(companyUseCase);
    }

    @Test
    @DisplayName("index returns list of companies wrapped in ApiResponse")
    void testIndex() {
        Company c = Company.builder()
                .id("comp-1")
                .companyId(1L)
                .name("UYFC")
                .code("UYFC")
                .isActive(true)
                .build();

        when(companyUseCase.listCompanies()).thenReturn(Flux.just(c));

        Mono<ApiResponse<List<CompanyResponse>>> responseMono = companyController.index();

        StepVerifier.create(responseMono)
                .assertNext(res -> {
                    assertTrue(res.isSuccess());
                    assertEquals(1, res.getData().size());
                    assertEquals("UYFC", res.getData().get(0).getName());
                })
                .verifyComplete();
    }
}
