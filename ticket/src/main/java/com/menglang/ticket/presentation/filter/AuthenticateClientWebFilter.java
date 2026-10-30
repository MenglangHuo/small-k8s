package com.menglang.ticket.presentation.filter;

import tools.jackson.databind.ObjectMapper;
import com.menglang.ticket.domain.port.in.ApiClientUseCase;
import com.menglang.ticket.presentation.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
@Component
@Order(-10)
@RequiredArgsConstructor
public class AuthenticateClientWebFilter implements WebFilter {

    private final ApiClientUseCase apiClientUseCase;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (!path.startsWith("/api/v1/client/") && !path.startsWith("/api/ticket/client")) {
            return chain.filter(exchange);
        }

        String clientId = request.getHeaders().getFirst("X-Client-ID");
        String clientSecret = request.getHeaders().getFirst("X-Client-Secret");

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return unauthorizedResponse(exchange, "Missing X-Client-ID or X-Client-Secret headers");
        }

        return apiClientUseCase.authenticate(clientId, clientSecret)
                .flatMap(client -> {
                    exchange.getAttributes().put("API_CLIENT", client);
                    exchange.getAttributes().put("COMPANY_ID", client.getCompanyId());
                    return chain.filter(exchange)
                            .contextWrite(Context.of("API_CLIENT", client, "COMPANY_ID", client.getCompanyId()));
                })
                .onErrorResume(e -> unauthorizedResponse(exchange, "Invalid API client credentials"));
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            ApiResponse<Void> errorResp = ApiResponse.error(message);
            byte[] bytes = objectMapper.writeValueAsBytes(errorResp);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }
}
