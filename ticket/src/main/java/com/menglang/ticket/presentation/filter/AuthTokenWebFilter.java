package com.menglang.ticket.presentation.filter;

import tools.jackson.databind.ObjectMapper;
import com.menglang.ticket.infrastructure.security.TokenService;
import com.menglang.ticket.infrastructure.security.TokenService.SessionInfo;
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

/**
 * Reactive security filter that enforces valid Bearer tokens for all protected API endpoints.
 * Populates authenticated user context into request attributes and reactive context.
 */
@Slf4j
@Component
@Order(-15)
@RequiredArgsConstructor
public class AuthTokenWebFilter implements WebFilter {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. Allow public authentication endpoints & actuator health pings
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 2. Allow client ingest endpoints authenticated via X-Client-ID & X-Client-Secret
        if (isClientIngestionEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 3. Protected endpoint: verify Authorization Bearer token
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Access denied to {}: missing or malformed Bearer token", path);
            return unauthorizedResponse(exchange, "Unauthorized: Missing or invalid Authorization Bearer token");
        }

        String token = authHeader.substring(7).trim();
        SessionInfo session = tokenService.validateSessionToken(token);

        if (session == null) {
            log.warn("Access denied to {}: invalid or expired session token", path);
            return unauthorizedResponse(exchange, "Unauthorized: Session expired or invalid token. Please log in.");
        }

        // 4. Inject authenticated user and tenant into exchange attributes and reactive context
        exchange.getAttributes().put("USER_ID", session.userId());
        exchange.getAttributes().put("COMPANY_ID", session.companyId());
        exchange.getAttributes().put("USER_ROLE", session.role());
        exchange.getAttributes().put("USERNAME", session.username());

        return chain.filter(exchange)
                .contextWrite(Context.of(
                        "USER_ID", session.userId(),
                        "COMPANY_ID", session.companyId(),
                        "USER_ROLE", session.role(),
                        "USERNAME", session.username()
                ));
    }

    private boolean isPublicEndpoint(String path) {
        return path.equals("/token")
                || path.equals("/login")
                || path.equals("/two-factor")
                || path.equals("/logout")
                || path.startsWith("/actuator");
    }

    private boolean isClientIngestionEndpoint(String path) {
        return path.startsWith("/api/v1/client/") || path.startsWith("/api/ticket/client");
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
            log.error("Failed to serialize unauthorized response", e);
            byte[] fallback = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes();
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(fallback);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}
