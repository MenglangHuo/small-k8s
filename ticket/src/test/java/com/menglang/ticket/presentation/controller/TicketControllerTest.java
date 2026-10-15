package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.Ticket;
import com.menglang.ticket.domain.model.TicketPriority;
import com.menglang.ticket.domain.model.TicketStatus;
import com.menglang.ticket.domain.model.TicketType;
import com.menglang.ticket.domain.port.in.TicketUseCase;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.TicketResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketUseCase ticketUseCase;

    private TicketController ticketController;

    @BeforeEach
    void setUp() {
        ticketController = new TicketController(ticketUseCase);
    }

    @Test
    @DisplayName("show returns ticket by ID wrapped in ApiResponse")
    void testShowTicket() {
        Ticket mockTicket = Ticket.builder()
                .id("mongo-id-1")
                .ticketKey("TCK-10")
                .companyId(1L)
                .title("Cannot login to portal")
                .description("Login button doesn't respond")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .type(TicketType.BUG)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(ticketUseCase.getTicketById(eq("TCK-10"), eq(1L), eq(true)))
                .thenReturn(Mono.just(mockTicket));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/tickets/TCK-10").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("COMPANY_ID", 1L);

        Mono<ApiResponse<TicketResponse>> responseMono = ticketController.show("TCK-10", true, exchange);

        StepVerifier.create(responseMono)
                .assertNext(apiResponse -> {
                    assertTrue(apiResponse.isSuccess());
                    assertNotNull(apiResponse.getData());
                    assertEquals("TCK-10", apiResponse.getData().getTicketKey());
                    assertEquals("Cannot login to portal", apiResponse.getData().getTitle());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("destroy deletes ticket and returns success ApiResponse")
    void testDestroyTicket() {
        when(ticketUseCase.deleteTicket(eq("TCK-10"), eq(1L), eq(1L)))
                .thenReturn(Mono.just(true));

        MockServerHttpRequest request = MockServerHttpRequest.delete("/api/v1/tickets/TCK-10").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("COMPANY_ID", 1L);
        exchange.getAttributes().put("USER_ID", 1L);

        Mono<ApiResponse<Void>> responseMono = ticketController.destroy("TCK-10", exchange);

        StepVerifier.create(responseMono)
                .assertNext(apiResponse -> {
                    assertTrue(apiResponse.isSuccess());
                    assertEquals("Deleted successfully.", apiResponse.getMessage());
                })
                .verifyComplete();
    }
}
