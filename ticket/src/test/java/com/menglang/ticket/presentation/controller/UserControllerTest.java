package com.menglang.ticket.presentation.controller;

import com.menglang.ticket.domain.model.User;
import com.menglang.ticket.domain.port.in.UserUseCase;
import com.menglang.ticket.presentation.response.ApiResponse;
import com.menglang.ticket.presentation.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserUseCase userUseCase;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userUseCase);
    }

    @Test
    @DisplayName("listAgents returns list of active agents")
    void testListAgents() {
        User agent = User.builder()
                .id("u-1")
                .userId(2L)
                .companyId(1L)
                .username("agent")
                .fullName("Support Agent")
                .email("agent@uyfc.org")
                .roleNames(List.of("AGENT"))
                .isActive(true)
                .build();

        when(userUseCase.listAgents(1L)).thenReturn(Flux.just(agent));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/agents").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("COMPANY_ID", 1L);

        Mono<ApiResponse<List<UserResponse>>> responseMono = userController.listAgents(exchange);

        StepVerifier.create(responseMono)
                .assertNext(res -> {
                    assertTrue(res.isSuccess());
                    assertEquals(1, res.getData().size());
                    assertEquals("Support Agent", res.getData().get(0).getFullName());
                })
                .verifyComplete();
    }
}
