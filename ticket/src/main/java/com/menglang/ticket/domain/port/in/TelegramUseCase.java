package com.menglang.ticket.domain.port.in;

import com.menglang.ticket.domain.model.TelegramSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TelegramUseCase {
    Mono<String> generateInviteLink(Long companyId, Long createdBy);
    Flux<TelegramSubscriber> listSubscribers(Long companyId, int page, int perPage);
    Mono<Map<String, Object>> getTelegramCounts(Long companyId);
    Mono<Long> getTotalInvitesCount(Long companyId);
    Mono<Long> getTotalSubscribersCount(Long companyId);
    Mono<TelegramSubscriber> updateSubscriberStatus(String chatId, Long companyId, Boolean isActive);
    Mono<Map<String, Object>> sendTestNotification(String chatId, Long companyId);
    Mono<Boolean> removeSubscriber(String chatId, Long companyId);
    Mono<Map<String, Object>> handleWebhook(Map<String, Object> updatePayload, String secretTokenHeader);
}
