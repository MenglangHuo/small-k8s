package com.menglang.ticket.application.service;

import com.menglang.ticket.domain.model.NotificationOutbox;
import com.menglang.ticket.domain.port.in.OutboxUseCase;
import com.menglang.ticket.domain.port.out.OutboxRepositoryPort;
import com.menglang.ticket.domain.port.out.TelegramPort;
import com.menglang.ticket.domain.port.out.TelegramSubscriberRepositoryPort;
import com.menglang.ticket.infrastructure.telegram.TelegramConfig;
import com.menglang.ticket.infrastructure.telegram.TelegramTicketFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxApplicationService implements OutboxUseCase {

    private final OutboxRepositoryPort outboxRepositoryPort;
    private final TelegramSubscriberRepositoryPort subscriberRepositoryPort;
    private final TelegramPort telegramPort;
    private final TelegramConfig telegramConfig;

    @Value("${ticket.outbox.batch-size:50}")
    private int batchSize;

    @Value("${ticket.outbox.max-attempts:5}")
    private int maxAttempts;

    @Override
    public Mono<Void> processPendingOutbox() {
        return outboxRepositoryPort.findPending(batchSize)
                .flatMap(this::processSingleOutboxMessage)
                .then();
    }

    private Mono<Void> processSingleOutboxMessage(NotificationOutbox message) {
        return outboxRepositoryPort.markProcessing(message.getId())
                .flatMap(processingMsg -> {
                    String formattedCard = TelegramTicketFormatter.formatTicketCreated(processingMsg.getPayload(), null);
                    Long companyId = processingMsg.getCompanyId() != null ? processingMsg.getCompanyId() : 1L;

                    return subscriberRepositoryPort.findActiveByCompanyId(companyId)
                            .map(sub -> sub.getTelegramChatId())
                            .collectList()
                            .flatMap(chatIds -> {
                                Set<String> targetChats = new HashSet<>(chatIds);
                                // Fallback to default chat ID if configured
                                if (telegramConfig.getDefaultChatId() != null && !telegramConfig.getDefaultChatId().isBlank()) {
                                    targetChats.add(telegramConfig.getDefaultChatId());
                                }

                                if (targetChats.isEmpty()) {
                                    log.info("No active Telegram subscribers found for company {}. Marking sent.", companyId);
                                    return outboxRepositoryPort.markSent(processingMsg.getId());
                                }

                                return Flux.fromIterable(targetChats)
                                        .flatMap(chatId -> telegramPort.sendMessage(chatId, formattedCard))
                                        .collectList()
                                        .flatMap(results -> {
                                            boolean anySuccess = results.stream().anyMatch(Boolean.TRUE::equals);
                                            if (anySuccess) {
                                                return outboxRepositoryPort.markSent(processingMsg.getId());
                                            } else {
                                                return outboxRepositoryPort.markFailed(processingMsg.getId(), "All telegram delivery attempts failed", maxAttempts);
                                            }
                                        });
                            });
                })
                .onErrorResume(e -> {
                    log.error("Outbox processing failed for id {}: {}", message.getId(), e.getMessage());
                    return outboxRepositoryPort.markFailed(message.getId(), e.getMessage(), maxAttempts);
                });
    }
}
