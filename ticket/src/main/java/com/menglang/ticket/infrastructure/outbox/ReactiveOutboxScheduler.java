package com.menglang.ticket.infrastructure.outbox;

import com.menglang.ticket.domain.port.in.OutboxUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ticket.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveOutboxScheduler {

    private final OutboxUseCase outboxUseCase;

    @Scheduled(fixedDelayString = "${ticket.outbox.polling-interval-ms:5000}")
    public void runOutboxPolling() {
        outboxUseCase.processPendingOutbox()
                .subscribe(
                        null,
                        error -> log.error("Error during reactive outbox polling: {}", error.getMessage())
                );
    }
}
