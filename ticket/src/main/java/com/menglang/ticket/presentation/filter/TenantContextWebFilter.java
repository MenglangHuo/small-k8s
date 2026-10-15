package com.menglang.ticket.presentation.filter;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
@Order(-5)
public class TenantContextWebFilter implements WebFilter {

    public static final String COMPANY_ID_KEY = "COMPANY_ID";
    public static final String USER_ID_KEY = "USER_ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        Long companyId = 1L;
        String companyHeader = request.getHeaders().getFirst("X-Company-ID");
        if (companyHeader != null && !companyHeader.isBlank()) {
            try {
                companyId = Long.parseLong(companyHeader.trim());
            } catch (NumberFormatException ignored) {}
        } else if (exchange.getAttributes().containsKey(COMPANY_ID_KEY)) {
            companyId = (Long) exchange.getAttributes().get(COMPANY_ID_KEY);
        }

        Long userId = 1L;
        String userHeader = request.getHeaders().getFirst("X-User-ID");
        if (userHeader != null && !userHeader.isBlank()) {
            try {
                userId = Long.parseLong(userHeader.trim());
            } catch (NumberFormatException ignored) {}
        }

        exchange.getAttributes().put(COMPANY_ID_KEY, companyId);
        exchange.getAttributes().put(USER_ID_KEY, userId);

        return chain.filter(exchange)
                .contextWrite(Context.of(COMPANY_ID_KEY, companyId, USER_ID_KEY, userId));
    }
}
