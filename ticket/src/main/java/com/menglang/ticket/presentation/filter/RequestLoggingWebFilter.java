package com.menglang.ticket.presentation.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Global WebFilter that logs all inbound HTTP requests and outbound responses
 * with execution latency, status codes, query strings, and client metadata.
 */
@Slf4j
@Component
@Order(-100)
public class RequestLoggingWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Keep actuator healthcheck noise at trace level so docker logs remain clean
        boolean isActuator = path.startsWith("/actuator");
        long startTime = System.currentTimeMillis();

        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String query = request.getURI().getRawQuery();
        String fullPath = (query != null && !query.isBlank()) ? path + "?" + query : path;
        String clientIp = resolveClientIp(request);
        String clientId = request.getHeaders().getFirst("X-Client-ID");
        String companyId = request.getHeaders().getFirst("X-Company-ID");

        StringBuilder meta = new StringBuilder();
        if (clientId != null && !clientId.isBlank()) {
            meta.append(" [Client: ").append(clientId).append("]");
        }
        if (companyId != null && !companyId.isBlank()) {
            meta.append(" [Company: ").append(companyId).append("]");
        }

        if (isActuator) {
            log.trace("[HTTP IN ] {} {} from {}{}", method, fullPath, clientIp, meta);
        } else {
            log.info("[HTTP IN ] {} {} from {}{}", method, fullPath, clientIp, meta);
        }

        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    int statusCode = status != null ? status.value() : 200;

                    if (isActuator) {
                        log.trace("[HTTP OUT] {} {} {} ({}ms)", statusCode, method, fullPath, duration);
                    } else if (statusCode >= 400) {
                        log.warn("[HTTP OUT] {} {} {} ({}ms)", statusCode, method, fullPath, duration);
                    } else {
                        log.info("[HTTP OUT] {} {} {} ({}ms)", statusCode, method, fullPath, duration);
                    }
                })
                .doOnError(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("[HTTP ERR] {} {} failed in {}ms: {}", method, fullPath, duration, throwable.getMessage());
                });
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "127.0.0.1";
    }
}
