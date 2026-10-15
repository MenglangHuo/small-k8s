package com.menglang.ticket.infrastructure.telegram;

import com.menglang.ticket.domain.port.out.TelegramPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class TelegramWebClientAdapter implements TelegramPort {

    private final WebClient webClient;
    private final TelegramConfig config;
    private final CircuitBreaker circuitBreaker;

    public TelegramWebClientAdapter(WebClient.Builder webClientBuilder,
                                   TelegramConfig config,
                                   CircuitBreakerRegistry circuitBreakerRegistry) {
        this.config = config;
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("telegram_api");
    }

    @Override
    public Mono<Boolean> sendMessage(String chatId, String htmlMessage) {
        if (config.getBotToken() == null || config.getBotToken().isBlank()) {
            log.warn("Telegram bot token not configured. Skipping sendMessage to chatId: {}", chatId);
            return Mono.just(false);
        }

        String path = String.format("/bot%s/sendMessage", config.getBotToken());
        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", htmlMessage,
                "parse_mode", "HTML"
        );

        return webClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .map(res -> Boolean.TRUE.equals(res.get("ok")))
                .doOnSuccess(ok -> log.info("Telegram sendMessage to {} response ok: {}", chatId, ok))
                .doOnError(e -> log.error("Telegram sendMessage to {} failed: {}", chatId, e.getMessage()))
                .onErrorReturn(false);
    }
}
